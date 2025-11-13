package com.bbmovie.auth.security.oauth2.strategy.request.customizer;

import com.bbmovie.auth.utils.PkceUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component("x")
public class XOAuth2RequestCustomizer implements OAuth2RequestCustomizer {

    @Override
    public String getRegistrationId() {
        return "x";
    }

    @Override
    public void customize(Map<String, Object> parameters) {
        String codeVerifier = PkceUtils.generateCodeVerifier();
        String codeChallenge = PkceUtils.generateCodeChallenge(codeVerifier);

        parameters.put("code_challenge", codeChallenge);
        parameters.put("code_challenge_method", "S256");

        Map<String, String> state = new HashMap<>();
        state.put("code_verifier", codeVerifier);

        String stateJson = Base64.getUrlEncoder().encodeToString(
                state.toString().getBytes(StandardCharsets.UTF_8)
        );
        parameters.put("state", stateJson);
    }

}
