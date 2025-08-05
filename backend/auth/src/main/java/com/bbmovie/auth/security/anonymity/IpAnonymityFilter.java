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

/**
 * The {@code IpAnonymityFilter} class is a Spring component that acts as a servlet filter.
 * It extends the {@code OncePerRequestFilter} and is responsible for filtering incoming HTTP
 * requests to block access from anonymous IP addresses, such as those using VPNs or proxies.
 * <p>
 * This filter integrates with an {@code AnonymityCheckService} to determine whether a given
 * IP address is identified as anonymous. If the IP is determined to be anonymous, the
 * filter responds with an HTTP 403 Forbidden status and terminates the processing of the request.
 * <p>
 * Dependencies:
 * - {@code AnonymityCheckService}: A service that checks whether an IP address is anonymous.
 * <p>
 * The filter is designed to execute once per request and is automatically registered in Spring's
 * filter chain.
 */
@Component
public class IpAnonymityFilter extends OncePerRequestFilter {

    private final AnonymityCheckService anonymityCheckService;

    @Autowired
    public IpAnonymityFilter(AnonymityCheckService anonymityCheckService) {
        this.anonymityCheckService = anonymityCheckService;
    }

    /**
     * Filters incoming HTTP requests and blocks access for requests originating from anonymous IP addresses
     * (such as those using VPNs or proxies). If the IP address is identified as anonymous, the filter
     * responds with an HTTP 403 Forbidden status and terminates further processing of the request.
     *
     * @param request the {@code HttpServletRequest} object that contains the request the client has made
     *                to the servlet
     * @param response the {@code HttpServletResponse} object that contains the response the servlet
     *                 sends to the client
     * @param filterChain the {@code FilterChain} used to pass the request and response to the
     *                    next entity in the filter chain
     * @throws ServletException if an error occurs during the processing of the request
     * @throws IOException if an I/O error occurs during the processing of the request
     */
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
