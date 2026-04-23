package bbmovie.auth.auth_jwt_core.jwks;

import bbmovie.auth.auth_jwt_core.model.CachedJwks;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryJwksCache implements JwksCache {
    private final AtomicReference<CachedJwks> value = new AtomicReference<>();

    @Override
    public Optional<CachedJwks> get() {
        return Optional.ofNullable(value.get());
    }

    @Override
    public void put(CachedJwks jwks) {
        value.set(jwks);
    }

    @Override
    public void evict() {
        value.set(null);
    }
}
