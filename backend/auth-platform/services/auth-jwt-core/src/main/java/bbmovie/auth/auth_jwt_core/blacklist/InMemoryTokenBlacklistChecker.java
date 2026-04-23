package bbmovie.auth.auth_jwt_core.blacklist;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTokenBlacklistChecker implements TokenBlacklistChecker {
    private final Set<String> revokedSids = ConcurrentHashMap.newKeySet();
    private final Set<String> revokedJtis = ConcurrentHashMap.newKeySet();

    public void revokeSid(String sid) {
        if (sid != null && !sid.isBlank()) {
            revokedSids.add(sid);
        }
    }

    public void revokeJti(String jti) {
        if (jti != null && !jti.isBlank()) {
            revokedJtis.add(jti);
        }
    }

    @Override
    public boolean isRevoked(String sid, String jti) {
        return (sid != null && revokedSids.contains(sid))
                || (jti != null && revokedJtis.contains(jti));
    }
}
