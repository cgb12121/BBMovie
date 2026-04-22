package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.helper;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

public final class HttpHeaderExtractor {

    private HttpHeaderExtractor() {
    }

    public static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> out = new HashMap<>();
        var names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            out.put(name, request.getHeader(name));
        }
        return out;
    }
}

