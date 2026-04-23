package bbmovie.auth.mfa_service.repository;

import bbmovie.auth.mfa_service.domain.OtpChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallengeEntity, String> {
    Optional<OtpChallengeEntity> findByCode(String code);

    void deleteByExpiresAtBefore(Instant instant);
}
