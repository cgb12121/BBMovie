package com.bbmovie.auth.service.auth.mfa;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class TotpAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final TotpService totpService;

    @Autowired
    public TotpAuthenticationProvider(UserRepository userRepository, TotpService totpService) {
        this.userRepository = userRepository;
        this.totpService = totpService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String totpCode = authentication.getCredentials().toString();

        User user = userRepository.findByDisplayedUsername(username)
                .orElseThrow(() -> new AuthenticationServiceException("Something went wrong."));

        if (!user.isMfaEnabled()) {
            throw new DisabledException("MFA not enabled for user");
        }

        boolean isValid = totpService.verifyCode(totpCode, user.getTotpSecret());
        if (!isValid) {
            throw new BadCredentialsException("Invalid TOTP code");
        }

        return UsernamePasswordAuthenticationToken.authenticated(username, "", authentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}