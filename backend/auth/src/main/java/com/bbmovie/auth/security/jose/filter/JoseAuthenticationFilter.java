package com.bbmovie.auth.security.jose.filter;

import com.bbmovie.auth.exception.BlacklistedJwtTokenException;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.common.entity.JoseConstraint.JosePayload.*;
import static com.example.common.entity.JoseConstraint.JosePayload.ABAC.IS_ACCOUNTING_ENABLED;

@Log4j2
@Component
@RequiredArgsConstructor
@SuppressWarnings("ConstantConditions")
public class JoseAuthenticationFilter extends OncePerRequestFilter {

    private final JoseProvider joseProvider;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!joseProvider.validateToken(token)) {
                log.warn("Failed to validate token");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
                return;
            }

            Map<String, Object> claims = joseProvider.getClaimsFromToken(token);
            String sid = (String) claims.get(SID);
            String username = (String) claims.get(SUB);
            boolean isAccountEnabled = (Boolean) claims.get(IS_ACCOUNTING_ENABLED);

            if (joseProvider.isTokenInABACBlacklist(sid)) {
                response.setHeader("X-Auth-Error", "abac-policy-changed");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User's status changed, required new token.");
                return;
            }

            if (!isAccountEnabled) {
                log.warn("Account is Disabled for user: {}", username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account disabled");
                return;
            }

            if (joseProvider.isTokenInLogoutBlacklist(sid)) {
                throw new BlacklistedJwtTokenException("Access token has been blocked for this email and device");
            }

            List<GrantedAuthority> authorities = joseProvider.getRolesFromToken(token)
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

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth")
                || path.startsWith("/public")
                || path.startsWith("/.well-known/jwks.json")
                || path.contains("swagger")
                || path.contains("api-docs");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}