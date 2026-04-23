package bbmovie.auth.auth_jwt_spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public class AuthJwtSpringProperties {
    private AuthJwtMode mode = AuthJwtMode.AUTO;
    private String remoteJwksUri;
    private String devJwkPath = "classpath:jwk-dev.json";
    private String activeProfile = "default";
    private String roleClaim = "role";
    private String subscriptionTierClaim = "subscriptionTier";
    private String rolePrefix = "ROLE_";
    private String tierPrefix = "TIER_";
    private String defaultAuthority = "ROLE_ANONYMOUS";
    private String jwksCacheRedisKey = "auth:jwt:jwks:cache";
    private String blacklistSidRedisPrefix = "auth:jwt:blacklist:sid:";
    private String blacklistJtiRedisPrefix = "auth:jwt:blacklist:jti:";

    public AuthJwtMode getMode() {
        return mode;
    }

    public void setMode(AuthJwtMode mode) {
        this.mode = mode;
    }

    public String getRemoteJwksUri() {
        return remoteJwksUri;
    }

    public void setRemoteJwksUri(String remoteJwksUri) {
        this.remoteJwksUri = remoteJwksUri;
    }

    public String getDevJwkPath() {
        return devJwkPath;
    }

    public void setDevJwkPath(String devJwkPath) {
        this.devJwkPath = devJwkPath;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(String activeProfile) {
        this.activeProfile = activeProfile;
    }

    public String getRoleClaim() {
        return roleClaim;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    public String getSubscriptionTierClaim() {
        return subscriptionTierClaim;
    }

    public void setSubscriptionTierClaim(String subscriptionTierClaim) {
        this.subscriptionTierClaim = subscriptionTierClaim;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public String getTierPrefix() {
        return tierPrefix;
    }

    public void setTierPrefix(String tierPrefix) {
        this.tierPrefix = tierPrefix;
    }

    public String getDefaultAuthority() {
        return defaultAuthority;
    }

    public void setDefaultAuthority(String defaultAuthority) {
        this.defaultAuthority = defaultAuthority;
    }

    public String getJwksCacheRedisKey() {
        return jwksCacheRedisKey;
    }

    public void setJwksCacheRedisKey(String jwksCacheRedisKey) {
        this.jwksCacheRedisKey = jwksCacheRedisKey;
    }

    public String getBlacklistSidRedisPrefix() {
        return blacklistSidRedisPrefix;
    }

    public void setBlacklistSidRedisPrefix(String blacklistSidRedisPrefix) {
        this.blacklistSidRedisPrefix = blacklistSidRedisPrefix;
    }

    public String getBlacklistJtiRedisPrefix() {
        return blacklistJtiRedisPrefix;
    }

    public void setBlacklistJtiRedisPrefix(String blacklistJtiRedisPrefix) {
        this.blacklistJtiRedisPrefix = blacklistJtiRedisPrefix;
    }
}
