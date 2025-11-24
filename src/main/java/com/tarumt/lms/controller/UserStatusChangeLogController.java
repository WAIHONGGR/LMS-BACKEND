package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.UserStatusChangeLog;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.service.UserStatusChangeLogService;
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
@RequestMapping("/api/admin/status-change-logs")
@RequiredArgsConstructor
public class UserStatusChangeLogController {

    private final UserStatusChangeLogService logService;
    private final AdminService adminService;
    private final TokenVerifierUtils tokenVerifierUtils;

    /**
     * Get status change logs by user type (STUDENT or INSTRUCTOR) or userId
     * GET /api/admin/status-change-logs?userType=STUDENT
     * GET /api/admin/status-change-logs?userType=INSTRUCTOR
     * GET /api/admin/status-change-logs?userId=123
     * GET /api/admin/status-change-logs?userType=STUDENT&userId=123
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getStatusChangeLogs(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Long userId) {

        log.info("Received request to get status change logs (userType={}, userId={})", userType, userId);

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

            List<UserStatusChangeLog> logs;

            // If both userType and userId are provided, filter by both
            if (userType != null && !userType.isBlank() && userId != null) {
                // Validate userType
                String upperUserType = userType.toUpperCase();
                if (!upperUserType.equals("STUDENT") && !upperUserType.equals("INSTRUCTOR")) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "Invalid userType. Must be STUDENT or INSTRUCTOR", null));
                }
                logs = logService.getLogsByUserTypeAndUserId(upperUserType, userId);
            }
            // If only userId is provided, get logs for that user
            else if (userId != null) {
                logs = logService.getLogsByUserId(userId);
            }
            // If only userType is provided, get logs for that user type
            else if (userType != null && !userType.isBlank()) {
                String upperUserType = userType.toUpperCase();
                if (!upperUserType.equals("STUDENT") && !upperUserType.equals("INSTRUCTOR")) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "Invalid userType. Must be STUDENT or INSTRUCTOR", null));
                }
                logs = logService.getLogsByUserType(upperUserType);
            }
            // If neither is provided, return error (too many logs to return all)
            else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Either userType or userId must be provided", null));
            }

            // Transform logs to include admin name if needed (optional enhancement)
            List<Map<String, Object>> logResponses = logs.stream()
                    .map(log -> {
                        Map<String, Object> logMap = new HashMap<>();
                        logMap.put("logId", log.getLogId());
                        logMap.put("userType", log.getUserType());
                        logMap.put("userId", log.getUserId());
                        logMap.put("oldStatus", log.getOldStatus());
                        logMap.put("newStatus", log.getNewStatus());

                        // FIX: Extract Admin ID safely while session is still open
                        // Check if null first, then access the ID
                        if (log.getChangedByAdmin() != null) {
                            // Access the admin ID while session is open (inside @Transactional)
                            Long adminId = log.getChangedByAdmin().getAdminId();
                            logMap.put("changedByAdminId", adminId); // Return just the ID, not the entity
                        } else {
                            logMap.put("changedByAdminId", null);
                        }

                        logMap.put("changedAt", log.getChangedAt());
                        logMap.put("reason", log.getReason());
                        return logMap;
                    })
                    .toList();

            log.info("Successfully fetched {} status change logs (userType={}, userId={})",
                    logResponses.size(), userType, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Status change logs fetched successfully", logResponses));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error fetching status change logs (userType={}, userId={})", userType, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    /**
     * Get status change logs for a specific user
     * GET /api/admin/status-change-logs/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true) // IMPORTANT: Keep session open during method execution
    public ResponseEntity<ApiResponse<?>> getStatusChangeLogsByUserId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long userId) {

        log.info("Received request to get status change logs for userId={}", userId);

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

            List<UserStatusChangeLog> logs = logService.getLogsByUserId(userId);

            // Transform logs to response format
            List<Map<String, Object>> logResponses = logs.stream()
                    .map(log -> {
                        Map<String, Object> logMap = new HashMap<>();
                        logMap.put("logId", log.getLogId());
                        logMap.put("userType", log.getUserType());
                        logMap.put("userId", log.getUserId());
                        logMap.put("oldStatus", log.getOldStatus());
                        logMap.put("newStatus", log.getNewStatus());


                        // Check if null first, then access the ID
                        if (log.getChangedByAdmin() != null) {
                            // Access the admin ID while session is open (inside @Transactional)
                            Long adminId = log.getChangedByAdmin().getAdminId();
                            logMap.put("changedByAdminId", adminId); // Return just the ID, not the entity
                        } else {
                            logMap.put("changedByAdminId", null);
                        }

                        logMap.put("changedAt", log.getChangedAt());
                        logMap.put("reason", log.getReason());
                        return logMap;
                    })
                    .toList();

            log.info("Successfully fetched {} status change logs for userId={}", logResponses.size(), userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Status change logs fetched successfully", logResponses));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error fetching status change logs for userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }
}

