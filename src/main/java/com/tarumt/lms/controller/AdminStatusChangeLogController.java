package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.model.AdminStatusChangeLog;
import com.tarumt.lms.model.SuperAdmin;
import com.tarumt.lms.service.AdminStatusChangeLogService;
import com.tarumt.lms.service.user.SuperAdminService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/superadmin/admin-status-change-logs")
@RequiredArgsConstructor
public class AdminStatusChangeLogController {

    private final AdminStatusChangeLogService logService;
    private final TokenVerifierUtils tokenVerifierUtils;
    private final SuperAdminService superAdminService;

    // Helper method to validate SuperAdmin
    private Optional<SuperAdmin> validateSuperAdmin(String authorizationHeader) {
        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                return Optional.empty();
            }

            String email = (String) claims.get("email");
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }

            return superAdminService.getActiveByEmail(email);
        } catch (Exception e) {
            log.error("Error validating SuperAdmin: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get status change logs for all admins or a specific admin
     * GET /api/superadmin/admin-status-change-logs (get all logs)
     * GET /api/superadmin/admin-status-change-logs?adminId=123 (get logs for specific admin)
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAdminStatusChangeLogs(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) Long adminId) {

        log.info("Received request to get admin status change logs (adminId={})", adminId);

        try {
            Optional<SuperAdmin> superAdminOpt = validateSuperAdmin(authorizationHeader);
            if (superAdminOpt.isEmpty()) {
                log.warn("Unauthorized SuperAdmin access attempt");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "SuperAdmin privileges required", null));
            }

            List<AdminStatusChangeLog> logs;

            // If adminId is provided, get logs for that specific admin
            if (adminId != null) {
                logs = logService.getLogsByAdminId(adminId);
            } else {
                // If no adminId, get all logs
                logs = logService.getAllLogs();
            }

            // Transform logs to response format
            List<Map<String, Object>> logResponses = logs.stream()
                    .map(log -> {
                        Map<String, Object> logMap = new HashMap<>();
                        logMap.put("logId", log.getLogId());
                        logMap.put("adminId", log.getAdmin() != null ? log.getAdmin().getAdminId() : null);
                        logMap.put("oldStatus", log.getOldStatus() != null ? log.getOldStatus().toString() : null);
                        logMap.put("newStatus", log.getNewStatus() != null ? log.getNewStatus().toString() : null);
                        logMap.put("changedBySuperAdminId", log.getChangedBySuperAdmin() != null ? log.getChangedBySuperAdmin().getSuperAdminId() : null);
                        logMap.put("changedAt", log.getChangedAt());
                        logMap.put("reason", log.getReason());
                        return logMap;
                    })
                    .toList();

            if (adminId != null) {
                log.info("Successfully fetched {} admin status change logs for adminId={}", logResponses.size(), adminId);
            } else {
                log.info("Successfully fetched {} admin status change logs (all admins)", logResponses.size());
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "Admin status change logs fetched successfully", logResponses));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error fetching admin status change logs (adminId={})", adminId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    /**
     * Get status change logs for a specific admin (alternative endpoint)
     * GET /api/superadmin/admin-status-change-logs/admin/{adminId}
     */
    @GetMapping("/admin/{adminId}")
    @Transactional(readOnly = true) // IMPORTANT: Keep session open during method execution
    public ResponseEntity<ApiResponse<?>> getAdminStatusChangeLogsByAdminId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long adminId) {

        log.info("Received request to get admin status change logs for adminId={}", adminId);

        try {
            Optional<SuperAdmin> superAdminOpt = validateSuperAdmin(authorizationHeader);
            if (superAdminOpt.isEmpty()) {
                log.warn("Unauthorized SuperAdmin access attempt");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "SuperAdmin privileges required", null));
            }

            List<AdminStatusChangeLog> logs = logService.getLogsByAdminId(adminId);

            // Transform logs to response format
            List<Map<String, Object>> logResponses = logs.stream()
                    .map(log -> {
                        Map<String, Object> logMap = new HashMap<>();
                        logMap.put("logId", log.getLogId());
                        logMap.put("adminId", log.getAdmin() != null ? log.getAdmin().getAdminId() : null);
                        logMap.put("oldStatus", log.getOldStatus() != null ? log.getOldStatus().toString() : null);
                        logMap.put("newStatus", log.getNewStatus() != null ? log.getNewStatus().toString() : null);
                        logMap.put("changedBySuperAdminId", log.getChangedBySuperAdmin() != null ? log.getChangedBySuperAdmin().getSuperAdminId() : null);
                        logMap.put("changedAt", log.getChangedAt());
                        logMap.put("reason", log.getReason());
                        return logMap;
                    })
                    .toList();

            log.info("Successfully fetched {} admin status change logs for adminId={}", logResponses.size(), adminId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Admin status change logs fetched successfully", logResponses));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error fetching admin status change logs for adminId={}", adminId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }
}

