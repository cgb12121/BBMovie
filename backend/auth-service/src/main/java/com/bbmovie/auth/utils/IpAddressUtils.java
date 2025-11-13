package com.bbmovie.auth.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.client.RestTemplate;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Log4j2
public class IpAddressUtils {

    private IpAddressUtils() {}

    public static String getClientIp(HttpServletRequest request) {
        String[] headerKeys = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
        };

        for (String header : headerKeys) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }

        return request.getRemoteAddr();
    }

    private static final String[] PUBLIC_IP_SERVICES = {
            "https://checkip.amazonaws.com",
            "https://api.ipify.org",
            "https://icanhazip.com",
    };

    @SuppressWarnings("unused")
    public static String getPublicServerIp() {
        synchronized (IpAddressUtils.class) {
            for (String serviceUrl : PUBLIC_IP_SERVICES) {
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    String publicIp = restTemplate.getForObject(serviceUrl, String.class);
                    if (publicIp != null && !publicIp.trim().isEmpty()) {
                        publicIp = publicIp.trim();
                        return publicIp;
                    }
                } catch (Exception e) {
                    log.warn("Failed to getActiveProvider public IP from {}: {}", serviceUrl, e.getMessage());
                }
            }
            log.error("Unable to determine public server IP from any external service.");
            throw new IllegalArgumentException("Unable to determine public server IP from any external service.");
        }
    }

    @SuppressWarnings("unused")
    public static String getServerInternalPrivateIp() {
        try {
            return findServerIpFromInterfaces();
        } catch (SocketException e) {
            throw new IllegalArgumentException("Unable to determine server IP: " + e.getMessage());
        }
    }

    private static String findServerIpFromInterfaces() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (isValidNetworkInterface(ni)) {
                String ip = findIpAddressInInterface(ni);
                if (ip != null) {
                    return ip;
                }
            }
        }
        throw new IllegalArgumentException("Unable to determine server IP");
    }

    private static boolean isValidNetworkInterface(NetworkInterface ni) throws SocketException {
        return ni.isUp() && !ni.isLoopback() && !ni.isVirtual();
    }

    private static String findIpAddressInInterface(NetworkInterface ni) {
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            String ip = getValidIpAddress(addr);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String getValidIpAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()) {
            return null;
        }

        if (addr instanceof Inet4Address) {
            return addr.getHostAddress();
        }

        return null;
    }
}
