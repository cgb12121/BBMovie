package com.example.bbmovie.security.oauth2;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@PreAuthorize( "denyAll()")
@Deprecated(forRemoval = true)
@RestController("/api/auth")
public class ZaloOAuth {

    @GetMapping("/zalo/login")
    public void redirectToZalo(HttpServletResponse response) throws IOException {
        String clientId = "Not Yet Implemented";
        String redirectUri = URLEncoder.encode("http://localhost:8080/api/auth/zalo/callback", StandardCharsets.UTF_8);
        String state = UUID.randomUUID().toString();

        String zaloUrl = "https://oauth.zalo.me/oauth/v3/authorize"
                + "?app_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;

        response.sendRedirect(zaloUrl);
    }

    @GetMapping("/zalo/callback")
    public ResponseEntity<?> zaloCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        String accessToken = exchangeCodeForAccessToken(code);
        Object profile = fetchZaloProfile(accessToken);

        // TODO: Create/find user, issue JWT, set cookie etc.
        return ResponseEntity.ok(profile);
    }

    public String exchangeCodeForAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("app_id", "Not Yet Implemented");
        params.add("app_secret", "Not Yet Implemented");
        params.add("code", code);
        params.add("redirect_uri", "http://localhost:8080/api/auth/zalo/callback");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth.zalo.me/oauth/v3/access_token",
                new HttpEntity<>(params, new HttpHeaders()),
                Map.class
        );

        assert response.getBody() != null;
        return (String) response.getBody().get("access_token");
    }

    public Object fetchZaloProfile(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("access_token", accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                "https://graph.zalo.me/v2.0/me?fields=id,name,picture",
                HttpMethod.GET,
                entity,
                Object.class
        );

        return response.getBody();
    }
}
