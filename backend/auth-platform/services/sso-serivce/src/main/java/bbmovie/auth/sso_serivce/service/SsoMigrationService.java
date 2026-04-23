package bbmovie.auth.sso_serivce.service;

import bbmovie.auth.sso_serivce.domain.RefreshSessionEntity;
import bbmovie.auth.sso_serivce.dto.IdentityVerifyRequest;
import bbmovie.auth.sso_serivce.dto.IdentityVerifyResponse;
import bbmovie.auth.sso_serivce.dto.JwtPairResponse;
import bbmovie.auth.sso_serivce.dto.LoginRequest;
import bbmovie.auth.sso_serivce.dto.MfaVerifyTotpRequest;
import bbmovie.auth.sso_serivce.dto.MfaVerifyTotpResponse;
import bbmovie.auth.sso_serivce.repository.RefreshSessionRepository;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SsoMigrationService {
    private final RestClient.Builder restClientBuilder;
    private final JwtKeyService keyService;
    private final RefreshSessionRepository refreshSessionRepository;

    @Value("${sso.identity-base-url:http://localhost:8085}")
    private String identityBaseUrl;

    @Value("${sso.mfa-base-url:http://localhost:8086}")
    private String mfaBaseUrl;

    @Value("${sso.access-token-seconds:900}")
    private long accessTokenSeconds;

    @Value("${sso.refresh-token-seconds:604800}")
    private long refreshTokenSeconds;

    private final Set<String> revokedSids = ConcurrentHashMap.newKeySet();

    @Transactional
    public JwtPairResponse login(LoginRequest request) {
        IdentityVerifyResponse identity = restClientBuilder.build()
                .post()
                .uri(identityBaseUrl + "/auth/internal/users/verify-credentials")
                .body(new IdentityVerifyRequest(request.email(), request.password()))
                .retrieve()
                .body(IdentityVerifyResponse.class);

        if (identity == null || !identity.success()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (identity.isMfaEnabled()) {
            if (request.totpCode() == null || request.totpCode().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MFA code required");
            }
            MfaVerifyTotpResponse mfaResult = restClientBuilder.build()
                    .post()
                    .uri(mfaBaseUrl + "/internal/mfa/verify-totp")
                    .body(new MfaVerifyTotpRequest(identity.email(), request.totpCode()))
                    .retrieve()
                    .body(MfaVerifyTotpResponse.class);
            if (mfaResult == null || !mfaResult.valid()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
            }
        }

        String sid = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        String access = sign(identity.userId(), identity.email(), identity.role(), sid, UUID.randomUUID().toString(), accessTokenSeconds, "access");
        String refresh = sign(identity.userId(), identity.email(), identity.role(), sid, refreshJti, refreshTokenSeconds, "refresh");

        RefreshSessionEntity session = new RefreshSessionEntity();
        session.setSid(sid);
        session.setJti(refreshJti);
        session.setEmail(identity.email());
        session.setToken(refresh);
        session.setExpiryDate(Instant.now().plusSeconds(refreshTokenSeconds));
        session.setRevoked(false);
        refreshSessionRepository.save(session);

        return new JwtPairResponse(access, refresh, sid, identity.email(), identity.role());
    }

    @Transactional
    public JwtPairResponse refreshAccessToken(String bearerToken) throws ParseException {
        String token = stripBearer(bearerToken);
        SignedJWT jwt = parse(token);
        String sid = claimString(jwt, "sid");
        String email = claimString(jwt, "email");
        String role = claimString(jwt, "role");
        String userId = jwt.getJWTClaimsSet().getSubject();

        if (revokedSids.contains(sid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session revoked");
        }

        RefreshSessionEntity session = refreshSessionRepository.findByEmailAndSid(email, sid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session not found"));
        if (session.isRevoked() || session.getExpiryDate().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session expired");
        }

        String access = sign(userId, email, role, sid, UUID.randomUUID().toString(), accessTokenSeconds, "access");
        return new JwtPairResponse(access, session.getToken(), sid, email, role);
    }

    @Transactional
    public void logout(String bearerToken) {
        String token = stripBearer(bearerToken);
        SignedJWT jwt = parse(token);
        String sid = claimString(jwt, "sid");
        revokedSids.add(sid);
        refreshSessionRepository.findBySid(sid).ifPresent(session -> {
            session.setRevoked(true);
            refreshSessionRepository.save(session);
        });
    }

    public Map<String, Object> jwks() {
        return keyService.getPublicJwks();
    }

    public Map<String, Object> adminJwksAll() {
        return keyService.getPublicJwks();
    }

    public Map<String, Object> adminJwksActive() {
        return keyService.getPublicJwks();
    }

    private String sign(String userId, String email, String role, String sid, String jti, long ttlSeconds, String kind) {
        try {
            var key = keyService.getActiveKey();
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .claim("email", email)
                    .claim("role", role)
                    .claim("sid", sid)
                    .claim("kind", kind)
                    .jwtID(jti)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .keyID(key.getKeyID())
                    .build(), claims);
            jwt.sign(new RSASSASigner(key.toPrivateKey()));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private SignedJWT parse(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }
    }

    private String stripBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header");
        }
        return header.substring(7);
    }

    private String claimString(SignedJWT jwt, String name) {
        try {
            Object value = jwt.getJWTClaimsSet().getClaim(name);
            return value == null ? null : value.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token claims");
        }
    }
}
