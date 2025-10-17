package com.bbmovie.auth.security.jose.filter;

import com.bbmovie.auth.exception.BlacklistedJwtTokenException;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import com.bbmovie.auth.security.jose.dto.JoseValidatedToken;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.common.entity.JoseConstraint.JosePayload.*;
import static com.example.common.entity.JoseConstraint.JosePayload.ABAC.IS_ACCOUNTING_ENABLED;

@Log4j2
@Component
@RequiredArgsConstructor
@SuppressWarnings("ConstantConditions") // Suppress warning on passing Nonnull (on purpose)
public class JoseAuthenticationFilter extends OncePerRequestFilter {

    private final JoseProviderStrategyContext joseContext;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (token == null) {
                log.warn("No token provided, proceeding to next filter");
                filterChain.doFilter(request, response);
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

            String sid = validated.sid();
            String username = validated.username();
            boolean isAccountEnabled = validated.isEnabled();

            boolean isAbacStale = provider.isTokenInABACBlacklist(sid);
            if (isAbacStale) {
                response.setHeader("X-Auth-Error", "abac-policy-changed");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User's status changed, required new token.");
                return;
            }

            if (!isAccountEnabled) {
                log.warn("[Strategy: {}] Account is Disabled", provider.getClass().getName());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (provider.isTokenInLogoutBlacklist(sid)) {
                throw new BlacklistedJwtTokenException("Access token has been blocked for this email and device");
            }

            List<GrantedAuthority> authorities = validated.roles()
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    username, null, authorities
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

    private JoseValidatedToken resolveAndValidateToken(@Nullable String token) {
        if (StringUtils.isEmpty(token)) return null;
        return Stream.of(joseContext.getActiveProvider(), joseContext.getPreviousProvider())
                .filter(Objects::nonNull)
                .map(provider -> validateTokenWithProvider(token, provider))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private JoseValidatedToken validateTokenWithProvider(String token, JoseProviderStrategy provider) {
        if (provider == null || !provider.validateToken(token)) {
            return null;
        }

        try {
            Map<String, Object> claims = provider.getClaimsFromToken(token);
            String username = getClaimAsString(claims, SUB);
            String roleStr = getClaimAsString(claims, ROLE);
            String sid = getClaimAsString(claims, SID);
            boolean isEnabled = Boolean.parseBoolean(getClaimAsString(claims, IS_ACCOUNTING_ENABLED));

            if (username == null || roleStr == null || sid == null) {
                log.warn("Required claims missing in token for provider: {}", provider.getClass().getSimpleName());
                return null;
            }

            return new JoseValidatedToken(provider, sid, username, List.of(roleStr), isEnabled);
        } catch (Exception ex) {
            log.warn("Failed to parse claims from token using provider: {}", provider.getClass().getSimpleName(), ex);
            return null;
        }
    }

    private String getClaimAsString(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value != null ? value.toString() : null;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}