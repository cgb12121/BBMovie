package com.bbmovie.auth.entity;

import com.bbmovie.auth.entity.enumerate.AuthProvider;
import com.bbmovie.auth.entity.enumerate.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@SQLRestriction("is_enabled is null or is_enabled = false")
@Builder
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@With
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements UserDetails {
    @Column(name = "displayed_username", nullable = false)
    private String displayedUsername;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "is_enabled")
    private Boolean isEnabled = false;

    @Column(name = "is_account_non_expired")
    private Boolean isAccountNonExpired = true;

    @Column(name = "is_account_non_locked")
    private Boolean isAccountNonLocked = true;

    @Column(name = "is_credentials_non_expired")
    private Boolean isCredentialsNonExpired = true;

    @Column(name = "last_logged_in")
    private LocalDateTime lastLoginTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return isAccountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isAccountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isCredentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}