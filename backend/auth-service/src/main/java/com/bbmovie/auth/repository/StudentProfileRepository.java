package com.bbmovie.auth.repository;

import com.bbmovie.auth.entity.StudentProfile;
import com.bbmovie.auth.entity.enumerate.StudentVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

	Optional<StudentProfile> findByUserId(UUID userId);

	Optional<StudentProfile> findByUserEmail(String email);

	List<StudentProfile> findByApplyStudentStatusDateIsNotNullOrderByApplyStudentStatusDateDesc();

	List<StudentProfile> findByStudentVerificationStatusOrderByApplyStudentStatusDateDesc(StudentVerificationStatus status);

	List<StudentProfile> findByStudentTrueOrderByStudentStatusExpireAtDesc();
}
