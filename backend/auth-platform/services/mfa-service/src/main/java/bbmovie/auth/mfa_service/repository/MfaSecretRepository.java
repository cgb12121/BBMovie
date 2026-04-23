package bbmovie.auth.mfa_service.repository;

import bbmovie.auth.mfa_service.domain.MfaSecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MfaSecretRepository extends JpaRepository<MfaSecretEntity, String> {
    Optional<MfaSecretEntity> findByUserId(String userId);
}
