package com.tarumt.lms.repo;

import com.tarumt.lms.model.CourseStatusChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseStatusChangeLogRepository extends JpaRepository<CourseStatusChangeLog, Long> {

    // Find all logs for a specific course
    List<CourseStatusChangeLog> findByCourseIdOrderByChangedAtDesc(Long courseId);

    // Find all logs (for history view)
    @Query("SELECT l FROM CourseStatusChangeLog l JOIN FETCH l.changedByAdmin ORDER BY l.changedAt DESC")
    List<CourseStatusChangeLog> findAllWithAdmin();

    // Find logs by course ID with admin loaded
    @Query("SELECT l FROM CourseStatusChangeLog l JOIN FETCH l.changedByAdmin WHERE l.courseId = :courseId ORDER BY l.changedAt DESC")
    List<CourseStatusChangeLog> findByCourseIdWithAdmin(@Param("courseId") Long courseId);
}

