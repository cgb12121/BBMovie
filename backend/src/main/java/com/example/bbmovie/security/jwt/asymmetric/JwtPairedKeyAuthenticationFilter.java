package com.example.bbmovie.security.jwt.asymmetric;

import com.example.bbmovie.exception.BlacklistedJwtTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtPairedKeyAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenPairedKeyProvider jwtTokenPairedKeyProvider;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            String deviceId = getDeviceIdFromRequest(request);

            if (jwt != null && jwtTokenPairedKeyProvider.validateToken(jwt)) {
                if (jwtTokenPairedKeyProvider.isTokenBlacklisted(jwt)) {
                    throw new BlacklistedJwtTokenException("Token has been invalidated");
                }

                String username = jwtTokenPairedKeyProvider.getUsernameFromToken(jwt);
                if (jwtTokenPairedKeyProvider.isAccessTokenBlockedForEmailAndDevice(username, deviceId)) {
                    throw new BlacklistedJwtTokenException("Access token has been blocked for this email and device");
                }

                List<String> roles = jwtTokenPairedKeyProvider.getRolesFromToken(jwt);
                List<GrantedAuthority> authorities = roles.stream()
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
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        filterChain.doFilter(request, response);
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
        String deviceId = request.getParameter("X-DEVICE-ID");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return null;
        }
        return deviceId;
    }
}