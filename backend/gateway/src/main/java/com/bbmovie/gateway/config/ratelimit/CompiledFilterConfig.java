package com.bbmovie.gateway.config.ratelimit;

import java.util.regex.Pattern;

public record CompiledFilterConfig(Pattern pattern, Bucket4jConfigProperties.FilterConfig config) {}