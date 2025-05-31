package com.example.bbmovie.utils;

import com.example.bbmovie.exception.ServerIpException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

import java.net.*;
import java.util.Enumeration;

@Log4j2
public class IpAddressUtils {

    private IpAddressUtils() {}

    public static String getClientIp(HttpServletRequest request) {
        String[] headerKeys = {
            "REMOTE_ADDR",
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA"
        };

        for (String header : headerKeys) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }

        return request.getRemoteAddr();
    }

    public static String getServerIp() {
        try {
            return findServerIpFromInterfaces();
        } catch (SocketException e) {
            throw new ServerIpException("Unable to determine server IP: " + e.getMessage());
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
        throw new ServerIpException("Unable to determine server IP");
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
