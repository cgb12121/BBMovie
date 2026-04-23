package bbmovie.auth.auth_jwt_core.model;

import java.time.Instant;

public record CachedJwks(
        String jwksJson,
        Instant fetchedAt,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
