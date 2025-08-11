package com.bbmovie.payment.dto.request;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class CallbackRequestContext {
    private final HttpServletRequest httpServletRequest;
    private final Map<String, String> queryParams;
    private final Map<String, String> formParams;
    private final Map<String, String> headers;
    private final String rawBody;
    private final String contentType;
    private final String httpMethod;
}