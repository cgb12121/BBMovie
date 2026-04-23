package bbmovie.auth.auth_jwt_core.config;

import java.time.Duration;

public record AuthJwtCoreProperties(
        Duration jwksCacheTtl,
        Duration jwksRefreshTimeout,
        boolean forceRefreshOnKidMiss,
        boolean useLastKnownGoodJwks,
        boolean backgroundRefreshEnabled
) {
    public static AuthJwtCoreProperties defaults() {
        return new AuthJwtCoreProperties(
                Duration.ofMinutes(5),
                Duration.ofSeconds(5),
                true,
                true,
                false
        );
    }
}
