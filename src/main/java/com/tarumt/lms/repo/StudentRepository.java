package com.tarumt.lms.repo;

import com.tarumt.lms.model.Student;
import com.tarumt.lms.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);
    Optional<Student> findByUserId(UUID userId);
    Optional<Student> findByStudentId(Long studentId);
    Optional<Student> findByStudentIdAndStatus(Long studentId, AccountStatus status);

    Optional<Student> findByEmailAndStatus(String email, AccountStatus status);

    Optional<Student> findByUserIdAndStatus(UUID userId, AccountStatus status);


    // Find students by status
    List<Student> findByStatus(AccountStatus status);

    // Find students by name or email (case-insensitive search)
    List<Student> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);

    // Find students by status and (name or email) (case-insensitive search)
    // Note: This requires @Query annotation for proper OR grouping
    @Query("SELECT s FROM Student s WHERE s.status = :status AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Student> findByStatusAndNameOrEmailContainingIgnoreCase(
            @Param("status") AccountStatus status,
            @Param("search") String search);
}
