package com.example.bbmovie.audit;

import com.example.bbmovie.utils.IpAddressUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            String contentType = httpRequest.getContentType();
            String clientIp = IpAddressUtils.getClientIp(httpRequest);

            boolean isMultipart = false;
            if (contentType != null) {
                isMultipart = contentType.startsWith("multipart/form-data")
                        || contentType.contains("video")
                        || contentType.contains("image");
            }

            if (isMultipart) {
                log.info("\n [{}] [Ip: {}/{}] Incoming Multipart Request: {} {} {} \n Body: [multipart content omitted]",
                        LocalDateTime.now(), httpRequest.getRemoteAddr(), clientIp,
                        httpRequest.getMethod(), httpRequest.getRequestURI(),
                        httpRequest.getQueryString() != null ? "?" + httpRequest.getQueryString() : ""
                );
                chain.doFilter(httpRequest, response);
            } else {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
                String requestBody = cachedRequest.getReader()
                        .lines()
                        .collect(Collectors.joining(System.lineSeparator()));

                log.info("\n [{}] [Ip: {}/{}] Incoming Request: {} {}{} \n Body: {}",
                        LocalDateTime.now(), cachedRequest.getRemoteAddr(), clientIp,
                        cachedRequest.getMethod(), cachedRequest.getRequestURI(),
                        cachedRequest.getQueryString() != null ? "?" + cachedRequest.getQueryString() : "",
                        requestBody
                );
                chain.doFilter(cachedRequest, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
