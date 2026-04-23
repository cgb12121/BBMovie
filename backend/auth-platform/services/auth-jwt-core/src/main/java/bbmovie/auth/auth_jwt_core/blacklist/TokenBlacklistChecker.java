package bbmovie.auth.auth_jwt_core.blacklist;

public interface TokenBlacklistChecker {
    boolean isRevoked(String sid, String jti);
}
