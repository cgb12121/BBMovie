package com.example.bbmovie.security.jose;

import com.example.bbmovie.exception.BlacklistedJwtTokenException;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
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
public class JoseFilter extends OncePerRequestFilter {

    private final JoseProviderStrategyContext joseContext;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);
            String deviceName = getDeviceIdFromRequest(request);

            if (token != null) {
                JoseValidatedToken validated = resolveAndValidateToken(token);
                if (validated != null) {
                    JoseProviderStrategy provider = validated.provider();
                    log.info("[Strategy: {}] Validated JoseToken: {}", provider.getClass().getName(), validated);

                    if (provider.isAccessTokenBlacklistedForEmailAndDevice(validated.username(), deviceName)) {
                        throw new BlacklistedJwtTokenException("Access token has been blocked for this email and device");
                    }

                    List<GrantedAuthority> authorities = validated.roles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                            validated.username(), "", authorities
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, authorities
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                }
            }
        } catch (BlacklistedJwtTokenException ex) {
            log.warn("Blacklisted token detected: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            return;
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token.");
            return;
        }

        filterChain.doFilter(request, response);
    }

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

    private String getDeviceIdFromRequest(HttpServletRequest request) {
        String deviceName = request.getHeader("X-DEVICE-NAME");
        if (deviceName != null && !deviceName.isBlank()) {
            return StringUtils.deleteWhitespace(deviceName);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("deviceName".equals(cookie.getName())) {
                    return StringUtils.deleteWhitespace(cookie.getValue());
                }
            }
        }
        return null;
    }
}