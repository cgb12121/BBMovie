package com.bbmovie.auth.entity.jose;

import com.bbmovie.auth.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "device_os", nullable = false)
    private String deviceOs;

    @Column(name = "device_ip_address", nullable = false)
    private String deviceIpAddress;

    @Column(name = "browser", nullable = false)
    private String browser;

    @Column(name = "browser_version", nullable = false)
    private String browserVersion;

    @Column(length = 1000, nullable = false)
    private String token;

    @Column(name = "jti", nullable = false)
    private String jti;

    @Column(name = "sid", nullable = false, unique = true)
    private String sid;

    @Column(nullable = false)
    private Date expiryDate;

    private boolean revoked;
}
