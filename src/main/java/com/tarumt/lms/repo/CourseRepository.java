package com.tarumt.lms.repo;

import com.tarumt.lms.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory WHERE c.courseId = :courseId")
    Optional<Course> findByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory WHERE c.instructor.instructorId = :instructorId")
    List<Course> findByInstructor_InstructorId(@Param("instructorId") Long instructorId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory WHERE c.status = :status")
    List<Course> findByStatus(@Param("status") String status);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory " +
            "WHERE c.status = :status AND c.instructor IS NOT NULL AND c.courseCategory IS NOT NULL")
    List<Course> findByStatusWithInstructorAndCategory(@Param("status") String status);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory WHERE c.instructor.instructorId = :instructorId AND c.status = :status")
    List<Course> findByInstructor_InstructorIdAndStatus(@Param("instructorId") Long instructorId, @Param("status") String status);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory WHERE c.courseCategory.courseCategoryId = :courseCategoryId")
    List<Course> findByCourseCategory_CourseCategoryId(@Param("courseCategoryId") Long courseCategoryId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Course> findByTitleContainingIgnoreCase(@Param("title") String title);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.courseCategory " +
            "WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
            "AND c.status = 'Active' AND c.instructor IS NOT NULL AND c.courseCategory IS NOT NULL")
    List<Course> findByTitleContainingIgnoreCaseAndComplete(@Param("title") String title);
}

