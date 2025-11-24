package com.tarumt.lms.repo;

import com.tarumt.lms.model.Enrollment;
import com.tarumt.lms.model.EnrollmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {

    // Find enrollment by student and course
    @Query("SELECT e FROM Enrollment e " +
           "LEFT JOIN FETCH e.student " +
           "LEFT JOIN FETCH e.course " +
           "WHERE e.id.studentId = :studentId AND e.id.courseId = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    // Find all enrollments for a student
    @Query("SELECT e FROM Enrollment e " +
           "LEFT JOIN FETCH e.student " +
           "LEFT JOIN FETCH e.course " +
           "LEFT JOIN FETCH e.course.instructor " +
           "LEFT JOIN FETCH e.course.courseCategory " +
           "WHERE e.id.studentId = :studentId")
    List<Enrollment> findByStudentId(@Param("studentId") Long studentId);

    // Find all enrollments for a course
    @Query("SELECT e FROM Enrollment e " +
           "LEFT JOIN FETCH e.student " +
           "LEFT JOIN FETCH e.course " +
           "WHERE e.id.courseId = :courseId")
    List<Enrollment> findByCourseId(@Param("courseId") Long courseId);

    // Check if student is enrolled in a course
    boolean existsById_StudentIdAndId_CourseId(Long studentId, Long courseId);

    // Find enrollments by student and status
    @Query("SELECT e FROM Enrollment e " +
           "LEFT JOIN FETCH e.student " +
           "LEFT JOIN FETCH e.course " +
           "LEFT JOIN FETCH e.course.instructor " +
           "LEFT JOIN FETCH e.course.courseCategory " +
           "WHERE e.id.studentId = :studentId AND e.status = :status")
    List<Enrollment> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") String status);
}

