package bbmovie.auth.identity_service.repository;

import bbmovie.auth.identity_service.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByDisplayedUsername(String displayedUsername);

    boolean existsByEmail(String email);
}
