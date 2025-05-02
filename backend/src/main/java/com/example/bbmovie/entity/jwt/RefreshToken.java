package com.example.bbmovie.entity.jwt;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000, nullable = false)
    private String token;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Date expiryDate;

    private boolean revoked;

    public RefreshToken(String token, String email, Date expiryDate) {
        this.token = token;
        this.email = email;
        this.expiryDate = expiryDate;
        this.revoked = false;
    }
}
