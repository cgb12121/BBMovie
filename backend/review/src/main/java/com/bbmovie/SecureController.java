package com.bbmovie;

import io.micronaut.context.annotation.Context;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

@Controller("/secure")
public class SecureController {

    @Get("/all")
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> hello() {
        Map<String, String> map = new HashMap<>();
        map.put("message", "Hello World");
        return map;
    }

    @Get("/secure")
    @Produces(MediaType.TEXT_PLAIN)
    @Secured({"ROLE_ADMIN", "ADMIN"})
    public String secured(Authentication authentication) {
        return "Access granted!" + authentication.getAttributes().toString();
    }

    @Get("/admin")
    @Produces(MediaType.TEXT_PLAIN)
    @Secured({SecurityRule.IS_AUTHENTICATED, SecurityRule.IS_ANONYMOUS, "ROLE_ADMIN", "ADMIN"})
    public String admin(@Nullable @Context Authentication authentication) {
        if (authentication == null) {
            return "Anonymous access";
        }
        return "User:" + authentication.getName() + "Authenticated: " + authentication.getAttributes() + " with role: " + authentication.getRoles() + "or" + authentication.getAttributes().get("role");
    }
}
