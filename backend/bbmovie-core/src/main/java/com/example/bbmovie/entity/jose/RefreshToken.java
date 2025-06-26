package com.example.bbmovie.entity.jose;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    private Date expiryDate;

    private boolean revoked;
}
