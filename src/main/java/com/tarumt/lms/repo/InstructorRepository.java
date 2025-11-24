package com.tarumt.lms.repo;

import com.tarumt.lms.model.Instructor;
import com.tarumt.lms.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Optional<Instructor> findByEmail(String email);

    Optional<Instructor> findByUserId(UUID userId);

    Optional<Instructor> findByInstructorId(Long instructorId);

    boolean existsByEmail(String email);

    // Add this new method for filtering by status
    List<Instructor> findByStatus(AccountStatus status);

    // Find by email, excluding INACTIVE
    Optional<Instructor> findByEmailAndStatusNot(String email, AccountStatus status);

    // Find by userId, excluding INACTIVE
    Optional<Instructor> findByUserIdAndStatusNot(UUID userId, AccountStatus status);

    // Find instructor by ID, excluding INACTIVE status
    Optional<Instructor> findByInstructorIdAndStatusNot(Long instructorId, AccountStatus status);
}
