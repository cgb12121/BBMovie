package com.bbmovie.auth.security.jose;


import java.util.List;

record JoseValidatedToken(JoseProviderStrategy provider, String username, List<String> roles) {}
