package bbmovie.auth.sso_serivce.repository;

import bbmovie.auth.sso_serivce.domain.RefreshSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, String> {
    Optional<RefreshSessionEntity> findBySid(String sid);

    Optional<RefreshSessionEntity> findByEmailAndSid(String email, String sid);
}
