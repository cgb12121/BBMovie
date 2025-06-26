package com.example.bbmovie.security.jose;

import java.util.List;

record JoseValidatedToken(JoseProviderStrategy provider, String username, List<String> roles) {}
