package com.bbmovie.auth.entity;

import com.bbmovie.auth.entity.enumerate.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Builder
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements UserDetails {
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "displayed_username", nullable = false)
    private String displayedUsername;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Builder.Default
    @Column(name = "profile_picture_url")
    private String profilePictureUrl = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_1280.png";

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "region")
    private Region region;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier")
    private SubscriptionTier subscriptionTier = SubscriptionTier.FREE;

    @Builder.Default
    @Column(name = "parental_controls_enabled")
    private boolean parentalControlsEnabled = false;

    @Builder.Default
    @Column(name = "is_mfa_enabled")
    private boolean isMfaEnabled = false;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean isEnabled = false;

    @Builder.Default
    @Column(name = "is_account_non_expired")
    private Boolean isAccountNonExpired = true;

    @Builder.Default
    @Column(name = "is_account_non_locked")
    private Boolean isAccountNonLocked = true;

    @Builder.Default
    @Column(name = "is_credentials_non_expired")
    private Boolean isCredentialsNonExpired = true;

    @Column(name = "last_logged_in")
    private LocalDateTime lastLoginTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Builder.Default
    @Column(name = "is_student", nullable = false)
    private boolean isStudent = false;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "student_profile_id", nullable = true)
    @ToString.Exclude
    private transient StudentProfile studentProfile;

    @Builder.Default
    @Column(name = "is_soft_deleted")
    private boolean isSoftDeleted = false;

    @Override
    public String getUsername() {
        return this.email;
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}