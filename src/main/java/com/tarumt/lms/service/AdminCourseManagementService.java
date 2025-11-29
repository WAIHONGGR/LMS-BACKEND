package com.tarumt.lms.service;

import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.Course;
import com.tarumt.lms.model.CourseStatusChangeLog;
import com.tarumt.lms.repo.CourseRepository;
import com.tarumt.lms.repo.CourseStatusChangeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCourseManagementService {

    private final CourseRepository courseRepository;
    private final CourseStatusChangeLogRepository courseStatusChangeLogRepository;

    // =====================================================
    // GET ALL COURSES (OPTIONAL STATUS FILTER & SEARCH)
    // =====================================================
    @Transactional(readOnly = true)
    public List<Course> getAllCourses(String status, String search) {
        log.info("Fetching all courses (status={}, search={})", status, search);

        if (status != null && !status.isBlank() && search != null && !search.isBlank()) {
            // Filter by status and search
            return courseRepository.findByStatusAndTitleOrDescriptionContainingIgnoreCase(status, search);
        } else if (status != null && !status.isBlank()) {
            // Filter by status only
            return courseRepository.findByStatus(status);
        } else if (search != null && !search.isBlank()) {
            // Search only
            return courseRepository.findByTitleOrDescriptionContainingIgnoreCase(search);
        } else {
            // Get all courses (ordered by create date DESC)
            return courseRepository.findAllOrderByCreateDateDesc();
        }
    }

    // =====================================================
    // UPDATE COURSE STATUS (With Logging)
    // =====================================================
    @Transactional
    public void updateCourseStatus(Long courseId, String newStatus, String reason, Admin actingAdmin) {
        log.info("Updating course status: courseId={}, newStatus={}", courseId, newStatus);

        // Find the course
        Course course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Validate admin
        if (actingAdmin == null) {
            log.error("Attempted to change course status without a valid admin. courseId={}", courseId);
            throw new IllegalArgumentException("Acting admin must not be null");
        }

        // Get old status
        String oldStatus = course.getStatus();

        // Validate status change (only allow valid statuses)
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("New status cannot be null or empty");
        }

        // Check if status is actually changing
        if (oldStatus != null && oldStatus.equalsIgnoreCase(newStatus)) {
            log.warn("Course {} status is already {}", courseId, newStatus);
            throw new RuntimeException("Course status is already " + newStatus);
        }

        // Update course status
        course.setStatus(newStatus);
        courseRepository.save(course);

        // Create and save status change log
        CourseStatusChangeLog logEntry = new CourseStatusChangeLog();
        logEntry.setCourseId(courseId);
        logEntry.setOldStatus(oldStatus);
        logEntry.setNewStatus(newStatus);
        logEntry.setChangedByAdmin(actingAdmin);
        logEntry.setChangedAt(OffsetDateTime.now());
        logEntry.setReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        courseStatusChangeLogRepository.save(logEntry);

        log.info("Course id={} status updated from {} to {} by admin id={}",
                courseId, oldStatus, newStatus, actingAdmin.getAdminId());
    }

    // =====================================================
    // GET COURSE STATUS CHANGE LOGS (History)
    // =====================================================
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCourseStatusChangeLogs(Long courseId) {
        log.info("Fetching course status change logs: courseId={}", courseId);

        List<CourseStatusChangeLog> logs;
        if (courseId != null) {
            // Get logs for specific course
            logs = courseStatusChangeLogRepository.findByCourseIdWithAdmin(courseId);
        } else {
            // Get all logs
            logs = courseStatusChangeLogRepository.findAllWithAdmin();
        }

        log.info("Found {} course status change logs", logs.size());

        // Transform to history log format
        return logs.stream()
                .map(log -> {
                    Map<String, Object> logEntry = new java.util.HashMap<>();
                    logEntry.put("logId", log.getLogId());
                    logEntry.put("courseId", log.getCourseId());
                    logEntry.put("oldStatus", log.getOldStatus());
                    logEntry.put("newStatus", log.getNewStatus());
                    logEntry.put("changedByAdminId", log.getChangedByAdmin() != null ? log.getChangedByAdmin().getAdminId() : null);
                    logEntry.put("changedByAdminName", log.getChangedByAdmin() != null ? log.getChangedByAdmin().getName() : null);
                    logEntry.put("reason", log.getReason());
                    logEntry.put("changedAt", log.getChangedAt() != null ? log.getChangedAt().toInstant() : null);
                    return logEntry;
                })
                .collect(Collectors.toList());
    }
}

