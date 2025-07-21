package com.bbmovie.auth.security.anonymity;

import com.bbmovie.auth.utils.IpAddressUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IpAnonymityFilter extends OncePerRequestFilter {

    private final AnonymityCheckService anonymityCheckService;

    @Autowired
    public IpAnonymityFilter(AnonymityCheckService anonymityCheckService) {
        this.anonymityCheckService = anonymityCheckService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = IpAddressUtils.getClientIp(request);

        if (ip != null && anonymityCheckService.isAnonymous(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access denied: anonymous IP (VPN, proxy, etc.) not allowed.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
