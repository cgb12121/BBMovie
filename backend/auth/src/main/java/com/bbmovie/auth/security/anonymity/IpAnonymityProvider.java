package com.bbmovie.auth.security.anonymity;

public interface IpAnonymityProvider {
    boolean isAnonymity(String ip);
    String getName();
}
