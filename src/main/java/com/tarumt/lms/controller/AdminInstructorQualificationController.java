package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.dto.InstructorRequirementViewDTO;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.enums.QualificationStatus;
import com.tarumt.lms.service.user.AdminInstructorQualificationService;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.utility.TokenVerifierUtils;
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
@RequestMapping("/api/admin")
public class AdminInstructorQualificationController {

    @Autowired private AdminService adminService;
    @Autowired private AdminInstructorQualificationService adminInstructorQualificationService;
    @Autowired private TokenVerifierUtils tokenVerifierUtils;

    // ================================
    // GET All Instructor Requirements (Admin Table View - Basic Info Only)
    // ================================
    @GetMapping("/requirements")
    public ResponseEntity<ApiResponse<?>> getAllInstructorRequirements(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        log.info("Received request for all instructor requirements status={}, search={}", status, search);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                log.warn("Unauthorized request for all instructor requirements");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for email={} when fetching instructor requirements", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Admin privileges required", null));
            }

            List<InstructorRequirementViewDTO> requirements =
                    adminInstructorQualificationService.getAllInstructorRequirements(status, search);

            log.info("Successfully fetched {} instructor requirements status={}, search={}", requirements.size(), status, search);
            return ResponseEntity.ok(new ApiResponse<>(true, "Fetched successfully", requirements));

        } catch (Exception e) {
            log.error("Error fetching instructor requirements status={}, search={}", status, search, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // GET Qualification Details (For View Details Modal - With Signed URLs)
    // ================================
    @GetMapping("/requirements/{qualificationId}/details")
    public ResponseEntity<ApiResponse<?>> getQualificationDetails(
            @PathVariable Long qualificationId,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request for qualification details qualificationId={}", qualificationId);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                log.warn("Unauthorized request for qualification details qualificationId={}", qualificationId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for email={} when fetching qualification details qualificationId={}", email, qualificationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Admin privileges required", null));
            }

            InstructorRequirementViewDTO details =
                    adminInstructorQualificationService.getQualificationDetails(qualificationId);

            log.info("Return fetched qualification details qualificationId={}", qualificationId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Fetched successfully", details));

        } catch (Exception e) {
            log.error("Error fetching qualification details qualificationId={}", qualificationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // APPROVE Qualification
    // ================================
    @PostMapping("/requirements/{qualificationId}/approve")
    public ResponseEntity<ApiResponse<?>> approveQualification(
            @PathVariable Long qualificationId,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request to approve qualification qualificationId={}", qualificationId);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                log.warn("Unauthorized request to approve qualification qualificationId={}", qualificationId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid or expired token", null));
            }

            String emailFromToken = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(emailFromToken);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for email={} when approving qualification qualificationId={}", emailFromToken, qualificationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Admin privileges required", null));
            }

            adminInstructorQualificationService.updateQualificationStatus(qualificationId, QualificationStatus.VERIFIED,null,adminOpt.get());

            log.info("Successfully approved qualification qualificationId={}", qualificationId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Qualification approved", null));

        } catch (Exception e) {
            log.error("Error approving qualification qualificationId={}", qualificationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // REJECT Qualification
    // ================================
    @PostMapping("/requirements/{qualificationId}/reject")
    public ResponseEntity<ApiResponse<?>> rejectQualification(
            @PathVariable Long qualificationId,
            @RequestBody(required = false) Map<String, String> request,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request to reject qualification qualificationId={}", qualificationId);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                log.warn("Unauthorized request to reject qualification qualificationId={}", qualificationId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid or expired token", null));
            }

            String emailFromToken = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getByEmail(emailFromToken);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for email={} when rejecting qualification qualificationId={}", emailFromToken, qualificationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Admin privileges required", null));
            }

            // Extract rejection reason from request body
            String rejectionReason = null;
            if (request != null && request.containsKey("rejectionReason")) {
                rejectionReason = request.get("rejectionReason");
                if (rejectionReason != null && rejectionReason.trim().isEmpty()) {
                    rejectionReason = null;
                } else if (rejectionReason != null) {
                    rejectionReason = rejectionReason.trim();
                }
            }

            // Update qualification status with rejection reason
            adminInstructorQualificationService.updateQualificationStatus(qualificationId, QualificationStatus.REJECTED, rejectionReason,adminOpt.get());

            log.info("Successfully rejected qualification qualificationId={}, reason={}", qualificationId, rejectionReason != null ? "provided" : "not provided");
            return ResponseEntity.ok(new ApiResponse<>(true, "Qualification rejected", null));

        } catch (Exception e) {
            log.error("Error rejecting qualification qualificationId={}", qualificationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    // ================================
    // GET Qualification History (All processed qualifications)
    // ================================
    @GetMapping("/requirements/history")
    public ResponseEntity<ApiResponse<?>> getQualificationHistory(
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request for qualification history");

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                log.warn("Unauthorized request for qualification history");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid token", null));
            }

            String email = (String) claims.get("email");
            Optional<Admin> adminOpt = adminService.getActiveByEmail(email);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found for email={} when fetching qualification history", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Admin privileges required", null));
            }

            List<Map<String, Object>> history = adminInstructorQualificationService.getQualificationHistory();

            log.info("Successfully fetched {} qualification history records", history.size());
            return ResponseEntity.ok(new ApiResponse<>(true, "Qualification history fetched successfully", history));

        } catch (Exception e) {
            log.error("Error fetching qualification history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

}

