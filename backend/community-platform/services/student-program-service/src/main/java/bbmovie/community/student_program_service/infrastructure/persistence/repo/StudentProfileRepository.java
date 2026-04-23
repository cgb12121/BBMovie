package bbmovie.community.student_program_service.infrastructure.persistence.repo;

import bbmovie.community.student_program_service.domain.StudentVerificationStatus;
import bbmovie.community.student_program_service.infrastructure.persistence.entity.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, String> {
    Optional<StudentProfileEntity> findByUserId(String userId);

    List<StudentProfileEntity> findByApplyStudentStatusDateIsNotNullOrderByApplyStudentStatusDateDesc();

    List<StudentProfileEntity> findByStudentVerificationStatusOrderByApplyStudentStatusDateDesc(
            StudentVerificationStatus status
    );

    List<StudentProfileEntity> findByStudentTrueOrderByStudentStatusExpireAtDesc();
}
