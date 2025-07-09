package com.bbmovie.notification.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/secured")
public class TestSecured {

    @GET
    @RolesAllowed({"ROLE_USER", "USER"})
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST secured";
    }

    @GET
    @Path("/admin")
    @RolesAllowed({"ROLE_ADMIN", "ADMIN"})
    @Produces(MediaType.TEXT_PLAIN)
    public String hello2(@Context SecurityContext sc) {
        return "Hello from Quarkus REST secured 2 " + sc.getAuthenticationScheme() + "/ " + sc.getUserPrincipal();
    }
}
