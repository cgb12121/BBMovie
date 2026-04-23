package bbmovie.auth.sso_serivce.controller;

import bbmovie.auth.sso_serivce.dto.LoginRequest;
import bbmovie.auth.sso_serivce.config.CutoverProperties;
import bbmovie.auth.sso_serivce.service.SsoMigrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class SsoMigrationController {
    private final SsoMigrationService ssoService;
    private final CutoverProperties cutover;

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest body) {
        if (!cutover.isEnabled()) {
            return cutoverDisabled();
        }
        var result = ssoService.login(body);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "accessToken", result.accessToken(),
                "refreshToken", result.refreshToken(),
                "sid", result.sid(),
                "email", result.email(),
                "role", result.role()
        ));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authorization) {
        if (!cutover.isEnabled()) {
            return cutoverDisabled();
        }
        ssoService.logout(authorization);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logout successful"));
    }

    @PostMapping("/auth/access-token")
    public ResponseEntity<Map<String, Object>> refreshAccessToken(@RequestHeader("Authorization") String authorization) throws ParseException {
        if (!cutover.isEnabled()) {
            return cutoverDisabled();
        }
        var result = ssoService.refreshAccessToken(authorization);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "accessToken", result.accessToken(),
                "refreshToken", result.refreshToken(),
                "sid", result.sid()
        ));
    }

    @GetMapping("/auth/oauth2-callback")
    public ResponseEntity<Map<String, Object>> oauth2Callback() {
        if (!cutover.isEnabled()) {
            return cutoverDisabled();
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "OAuth2 callback placeholder for migration phase"));
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        if (!cutover.isEnabled()) {
            return cutoverDisabled();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(ssoService.jwks());
    }

    @GetMapping("/admin/jwks/all")
    public ResponseEntity<Map<String, Object>> adminJwksAll() {
        if (!cutover.isEnabled()) {
            return cutoverDisabled();
        }
        return ResponseEntity.ok(ssoService.adminJwksAll());
    }

    @GetMapping("/admin/jwks/active")
    public ResponseEntity<Map<String, Object>> adminJwksActive() {
        if (!cutover.isEnabled()) {
            return cutoverDisabled();
        }
        return ResponseEntity.ok(ssoService.adminJwksActive());
    }

    private ResponseEntity<Map<String, Object>> cutoverDisabled() {
        return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "message", "SSO cutover disabled. Route this endpoint to legacy auth-service."
        ));
    }
}
