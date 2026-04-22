package com.bbmovie.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 *  CDI Producer
 *  <p>
 *  This class will have a single responsibility: to extract the user's UUID from the
 *  SecurityIdentity. It will also handle potential errors, like an unauthenticated user or an invalid UUID in the security token, by throwing appropriate web
 *  exceptions. This centralizes the logic and makes it reusable.
 */
@RequestScoped
public class UserProducer {

    @Inject
    SecurityIdentity identity;

    @Produces
    public UUID userId() {
        if (identity.isAnonymous()) {
            return null;
        }
        try {
            return UUID.fromString(identity.getPrincipal().getName());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid user ID format in token", Response.Status.BAD_REQUEST);
        }
    }
}
