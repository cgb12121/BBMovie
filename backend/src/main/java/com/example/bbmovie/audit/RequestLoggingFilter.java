//package com.example.bbmovie.audit;
//
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//public class RequestLoggingFilter implements Filter {
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        if (request instanceof HttpServletRequest httpRequest) {
//            String contentType = httpRequest.getContentType();
//
//            boolean isMultipart = false;
//            if (contentType != null) {
//                isMultipart = contentType.startsWith("multipart/form-data")
//                        || contentType.contains("video")
//                        || contentType.contains("image");
//            }
//
//            if (isMultipart) {
//                log.info("\n [{}] [Ip: {}/{}] Incoming Multipart Request: {} {} {} \n Body: [multipart content omitted]",
//                        LocalDateTime.now(), httpRequest.getRemoteAddr(), getClientIP(),
//                        httpRequest.getMethod(), httpRequest.getRequestURI(),
//                        httpRequest.getQueryString() != null ? "?" + httpRequest.getQueryString() : ""
//                );
//                chain.doFilter(httpRequest, response);
//            } else {
//                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
//                String requestBody = cachedRequest.getReader()
//                        .lines()
//                        .collect(Collectors.joining(System.lineSeparator()));
//
//                log.info("\n [{}] [Ip: {}/{}] Incoming Request: {} {}{} \n Body: {}",
//                        LocalDateTime.now(), cachedRequest.getRemoteAddr(), getClientIP(),
//                        cachedRequest.getMethod(), cachedRequest.getRequestURI(),
//                        cachedRequest.getQueryString() != null ? "?" + cachedRequest.getQueryString() : "",
//                        requestBody
//                );
//                chain.doFilter(cachedRequest, response);
//            }
//        } else {
//            chain.doFilter(request, response);
//        }
//    }
//
//    private String getClientIP() {
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
//
//        String ipAddress = request.getHeader("X-Forwarded-For");
//        String unknownClient = "unknown";
//        if (ipAddress == null || ipAddress.isEmpty() || unknownClient.equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("Proxy-Client-IP");
//        }
//        if (ipAddress == null || ipAddress.isEmpty() || unknownClient.equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (ipAddress == null || ipAddress.isEmpty() || unknownClient.equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("HTTP_CLIENT_IP");
//        }
//        if (ipAddress == null || ipAddress.isEmpty() || unknownClient.equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
//        }
//        if (ipAddress == null || ipAddress.isEmpty() || unknownClient.equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getRemoteAddr();
//        }
//
//        if (ipAddress != null && ipAddress.contains(",")) {
//            ipAddress = ipAddress.split(",")[0].trim();
//        }
//
//        return ipAddress;
//    }
//}
