package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.BlacklistedJwtTokenException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.service.UserService;
import com.bbmovie.auth.service.auth.RefreshTokenService;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class JoseAuthenticationFilter extends OncePerRequestFilter {

    private final JoseProviderStrategyContext joseContext;
    private final UserService userService;
    private final RefreshTokenService  refreshTokenService;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (token == null) {
                log.warn("Token is null,  can't get access token");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            JoseValidatedToken validated = resolveAndValidateToken(token);
            if (validated == null) {
                log.warn("Failed to validate token");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            JoseProviderStrategy provider = validated.provider();
            log.info("[Strategy: {}] Validated JoseToken: {}", provider.getClass().getName(), validated);

            String sid = provider.getSidFromToken(token);
            String username = validated.username();

            if (provider.isTokenInLogoutBlacklist(sid)) {
                throw new BlacklistedJwtTokenException("Access token has been blocked for this email and device");
            }
            // we will check blacklist the sid since sid (on refresh token) are stored on database
            // easier to track than jti from access token
            boolean isAbacStale = provider.isTokenInABACBlacklist(sid);
            if (isAbacStale) {
                User userAfterAbacUpdate = userService.findByEmail(username)
                        .orElseThrow(() -> new UserNotFoundException("Unexpected username, cannot find user"));

                List<GrantedAuthority> authorities = userAfterAbacUpdate.getAuthorities()
                        .stream()
                        .map(GrantedAuthority.class::cast)
                        .toList();

                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        username, "", authorities
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities
                );

                String newAccessToken = provider.generateAccessToken(authentication, sid, userAfterAbacUpdate);
                String newRefreshTokenString = provider.generateRefreshToken(authentication, sid, userAfterAbacUpdate);
                refreshTokenService.saveRefreshToken(sid, newRefreshTokenString);

                provider.removeTokenFromABACBlacklist(sid);

                response.setHeader("X-New-Access-Token", newAccessToken);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Normal flow where there is no abac changes
                List<GrantedAuthority> authorities = validated.roles()
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        username, "", authorities
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (BlacklistedJwtTokenException ex) {
            log.warn("Blacklisted token detected: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            SecurityContextHolder.clearContext();
            return;
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token.");
            SecurityContextHolder.clearContext();
            return;
        }

        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
    }

    // Only hold basic role based access control, not hold abac
    private JoseValidatedToken resolveAndValidateToken(@Nullable String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }

        JoseProviderStrategy active = joseContext.getActiveProvider();
        log.debug("Active provider: {}", active);

        if (active.validateToken(token)) {
            return new JoseValidatedToken(
                    active,
                    active.getUsernameFromToken(token),
                    active.getRolesFromToken(token)
            );
        }
        JoseProviderStrategy previous = joseContext.getPreviousProvider();
        if (previous != null && previous.validateToken(token)) {
            return new JoseValidatedToken(
                    previous,
                    previous.getUsernameFromToken(token),
                    previous.getRolesFromToken(token)
            );
        }
        return null;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}