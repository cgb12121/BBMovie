package com.example.bbmovie.utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class JwtPairedKeyGeneratorUtil {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        System.out.println("Private Key (Base64 encoded): " + java.util.Base64.getEncoder().encodeToString(privateKey.getEncoded()));
        System.out.println("Public Key (Base64 encoded): " + java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded()));
    }
}