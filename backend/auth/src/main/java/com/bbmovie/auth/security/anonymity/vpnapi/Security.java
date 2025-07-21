package com.bbmovie.auth.security.anonymity.vpnapi;

import lombok.Data;

@Data
public class Security {
    private boolean vpn;
    private boolean proxy;
    private boolean tor;
    private boolean relay;
}