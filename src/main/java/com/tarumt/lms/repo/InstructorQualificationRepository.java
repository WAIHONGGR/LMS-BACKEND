package com.tarumt.lms.repo;

import com.tarumt.lms.model.Instructor;
import com.tarumt.lms.model.InstructorQualification;
import com.tarumt.lms.model.enums.QualificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructorQualificationRepository extends JpaRepository<InstructorQualification, Long> {

    List<InstructorQualification> findByInstructorInstructorId(Long instructorId);
    List<InstructorQualification> findByInstructor_InstructorIdOrderBySubmittedAtDesc(Long instructorId);

    // Add JOIN FETCH to load instructor
    @Query("SELECT q FROM InstructorQualification q JOIN FETCH q.instructor")
    List<InstructorQualification> findAllWithInstructor();

    // Add JOIN FETCH with status filter
    @Query("SELECT q FROM InstructorQualification q JOIN FETCH q.instructor WHERE q.status = :status")
    List<InstructorQualification> findByStatusWithInstructor(@Param("status") QualificationStatus status);

    List<InstructorQualification> findByInstructorOrderBySubmittedAtDesc(Instructor instructor);
}
