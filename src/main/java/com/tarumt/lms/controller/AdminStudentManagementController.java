package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.Student;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.service.user.AdminStudentManagementService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentManagementController {

    @Autowired
    private final AdminStudentManagementService adminStudentManagementService;

    @Autowired
    private final AdminService adminService;

    @Autowired
    private final TokenVerifierUtils tokenVerifierUtils;

    // =====================================================
    // GET ALL STUDENTS (OPTIONAL STATUS FILTER & SEARCH)
    // =====================================================
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllStudents(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        log.info("Admin request: Fetch students (status={}, search={})", status, search);

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

            AccountStatus filter = null;
            if (status != null && !status.isBlank()) {
                try {
                    filter = AccountStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "Invalid status filter", null));
                }
            }

            List<Student> students = adminStudentManagementService.getAllStudents(filter, search);
            log.info("Fetched {} students (status={}, search={})", students.size(), status, search);

            return ResponseEntity.ok(new ApiResponse<>(true, "Students fetched successfully", students));
        } catch (Exception e) {
            log.error("Error fetching students", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    // =====================================================
    // GET STUDENT DETAILS BY ID
    // =====================================================
    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<?>> getStudentDetails(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long studentId) {

        log.info("Admin request: Get student details studentId={}", studentId);

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

            Optional<Student> studentOpt = adminStudentManagementService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                log.warn("Student not found studentId={}", studentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Student not found", null));
            }

            Student student = studentOpt.get();

            // Build response with all student details
            Map<String, Object> response = new HashMap<>();
            response.put("studentId", student.getStudentId());
            response.put("userId", student.getUserId().toString());
            response.put("email", student.getEmail());
            response.put("name", student.getName());
            response.put("status", student.getStatus());
            response.put("phoneNum", student.getPhoneNum() != null ? student.getPhoneNum() : "");
            response.put("birthDate", student.getBirthDate() != null ? student.getBirthDate() : "");
            response.put("registeredDate", student.getRegisteredDate() != null ? student.getRegisteredDate() : "");
            response.put("endDate", student.getEndDate() != null ? student.getEndDate() : "");

            log.info("Successfully fetched student details studentId={}", studentId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Student details fetched successfully", response));
        } catch (Exception e) {
            log.error("Error fetching student details studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    // =====================================================
    // UPDATE STUDENT STATUS (ACTIVE / INACTIVE)
    // =====================================================
    @PostMapping("/{studentId}/status")
    public ResponseEntity<ApiResponse<?>> updateStudentStatus(
            @PathVariable Long studentId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request to update student status studentId={}", studentId);

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

            Admin admin = adminOpt.get();

            // Get status and reason from request
            String inputStatus = request.get("status");
            String reason = request.get("reason"); // Optional field for auditing

            if (inputStatus == null || inputStatus.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(false, "Status is required", null));
            }

            // Convert string to enum safely
            AccountStatus status;
            try {
                status = AccountStatus.valueOf(inputStatus.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(false,
                                "Invalid status. Allowed values: ACTIVE, INACTIVE, PENDING", null));
            }

            // Only allow ACTIVE or INACTIVE
            if (status != AccountStatus.ACTIVE && status != AccountStatus.INACTIVE) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(false,
                                "Invalid status. Must be ACTIVE or INACTIVE", null));
            }

            // Call service and pass reason + admin
            adminStudentManagementService.updateStudentStatus(studentId, status, admin, reason);

            return ResponseEntity.ok(new ApiResponse<>(true, "Student status updated successfully", null));
        } catch (RuntimeException e) {
            // Handle specific business logic exceptions (e.g., Student not found)
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                log.warn("Student not found: studentId={}, error={}", studentId, e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, e.getMessage(), null));
            }
            log.error("Runtime error updating student status studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage() != null ? e.getMessage() : "Failed to update student status", null));
        } catch (Exception e) {
            log.error("Unexpected error updating student status studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }
}

