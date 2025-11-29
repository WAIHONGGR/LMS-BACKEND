package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.Course;
import com.tarumt.lms.service.AdminCourseManagementService;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseManagementController {

    private final AdminCourseManagementService adminCourseManagementService;
    private final TokenVerifierUtils tokenVerifierUtils;
    private final AdminService adminService;

    // =====================================================
    // GET ALL COURSES (OPTIONAL STATUS FILTER & SEARCH)
    // =====================================================
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllCourses(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        log.info("Admin request: Fetch courses (status={}, search={})", status, search);

        try {
            // Validate token and extract claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Privileges required", null));
            }

            List<Course> courses = adminCourseManagementService.getAllCourses(status, search);
            log.info("Fetched {} courses (status={}, search={})", courses.size(), status, search);

            return ResponseEntity.ok(new ApiResponse<>(true, "Courses fetched successfully", courses));
        } catch (Exception e) {
            log.error("Error fetching courses", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    // =====================================================
    // UPDATE COURSE STATUS (With Logging)
    // =====================================================
    @PostMapping("/{courseId}/status")
    public ResponseEntity<ApiResponse<?>> updateCourseStatus(
            @PathVariable Long courseId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Admin request: Update course status courseId={}", courseId);

        try {
            // Validate token and extract claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Privileges required", null));
            }

            // Extract status and reason from request body
            String newStatus = request.get("status");
            String reason = request.get("reason");

            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Status is required", null));
            }

            // Validate status (allow: Active, Inactive, Archived, Deleted)
            String statusUpper = newStatus.trim();
            if (!statusUpper.equalsIgnoreCase("Active") &&
                    !statusUpper.equalsIgnoreCase("Inactive") &&
                    !statusUpper.equalsIgnoreCase("Archived") &&
                    !statusUpper.equalsIgnoreCase("Deleted")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid status. Allowed: Active, Inactive, Archived, Deleted", null));
            }

            // Update course status with logging
            adminCourseManagementService.updateCourseStatus(
                    courseId,
                    statusUpper,
                    reason != null && !reason.isBlank() ? reason.trim() : null,
                    adminOpt.get()
            );

            log.info("Successfully updated course status: courseId={}, newStatus={}", courseId, statusUpper);
            return ResponseEntity.ok(new ApiResponse<>(true, "Course status updated successfully", null));

        } catch (IllegalArgumentException e) {
            log.error("Invalid argument when updating course status: courseId={}", courseId, e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (RuntimeException e) {
            log.error("Error updating course status: courseId={}", courseId, e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating course status: courseId={}", courseId, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    // =====================================================
    // GET COURSE STATUS CHANGE LOGS (History)
    // =====================================================
    @GetMapping("/status-change-logs")
    public ResponseEntity<ApiResponse<?>> getCourseStatusChangeLogs(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) Long courseId) {

        log.info("Admin request: Fetch course status change logs courseId={}", courseId);

        try {
            // Validate token and extract claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Privileges required", null));
            }

            List<Map<String, Object>> logs = adminCourseManagementService.getCourseStatusChangeLogs(courseId);
            log.info("Fetched {} course status change logs", logs.size());

            return ResponseEntity.ok(new ApiResponse<>(true, "Course status change logs fetched successfully", logs));

        } catch (Exception e) {
            log.error("Error fetching course status change logs", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }
}

