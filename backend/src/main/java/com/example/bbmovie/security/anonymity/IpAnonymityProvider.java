package com.example.bbmovie.security.anonymity;

public interface IpAnonymityProvider {
    boolean isAnonymity(String ip);
    String getName();
}
