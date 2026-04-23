package bbmovie.auth.auth_jwt_core.jwks;

import bbmovie.auth.auth_jwt_core.model.CachedJwks;

import java.util.Optional;

public interface JwksCache {
    Optional<CachedJwks> get();

    void put(CachedJwks jwks);

    void evict();
}
