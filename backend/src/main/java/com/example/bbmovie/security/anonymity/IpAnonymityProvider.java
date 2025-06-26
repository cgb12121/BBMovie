package com.example.bbmovie.security.anonymity;

public interface AnonymityDetectionStrategy {
    boolean isAnonymity(String ip);
}