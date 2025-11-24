package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.dto.InstructorDetailViewDTO;
import com.tarumt.lms.dto.InstructorListViewDTO;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.service.user.AdminInstructorManagementService;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminInstructorManagementController {

    @Autowired private AdminService adminService;
    @Autowired private AdminInstructorManagementService adminInstructorManagementService;
    @Autowired private TokenVerifierUtils tokenVerifierUtils;

    // ================================
    // GET All Instructors (Basic Info)
    // ================================
    @GetMapping("/instructors")
    public ResponseEntity<ApiResponse<?>> getAllInstructors(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        log.info("Received request for all instructors status={}, search={}", status, search);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                log.warn("Unauthorized request for all instructors");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for email={} when fetching instructors", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Admin privileges required", null));
            }

            List<InstructorListViewDTO> instructors =
                    adminInstructorManagementService.getAllInstructors(status, search);

            log.info("Successfully fetched {} instructors status={}, search={}", instructors.size(), status, search);
            return ResponseEntity.ok(new ApiResponse<>(true, "Fetched successfully", instructors));

        } catch (Exception e) {
            log.error("Error fetching instructors status={}, search={}", status, search, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // GET Instructor by ID (Full Details)
    // ================================
    @GetMapping("/instructors/{instructorId}")
    public ResponseEntity<ApiResponse<?>> getInstructorById(
            @PathVariable Long instructorId,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request for instructor details instructorId={}", instructorId);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                log.warn("Unauthorized request for instructor details instructorId={}", instructorId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for email={} when fetching instructor details instructorId={}", email, instructorId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Admin privileges required", null));
            }

            InstructorDetailViewDTO details = adminInstructorManagementService.getInstructorDetailsForAdmin(instructorId);

            log.info("Successfully fetched instructor details instructorId={}", instructorId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Fetched successfully", details));

        } catch (Exception e) {
            log.error("Error fetching instructor details instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // Active / Inactive / Pending instructor Account
    // ================================
    @PostMapping("/instructors/{instructorId}/status")
    public ResponseEntity<ApiResponse<?>> updateInstructorStatus(
            @PathVariable Long instructorId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request to update instructor status instructorId={}", instructorId);

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
            String reason = request.get("reason"); // new field for auditing

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
            adminInstructorManagementService.updateInstructorStatus(instructorId, status, admin, reason);

            return ResponseEntity.ok(new ApiResponse<>(true, "Instructor status updated successfully", null));

        } catch (Exception e) {
            log.error("Unexpected error updating instructor status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


}
