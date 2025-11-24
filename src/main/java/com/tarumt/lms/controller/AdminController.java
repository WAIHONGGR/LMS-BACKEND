package com.tarumt.lms.controller;

import com.tarumt.lms.dto.AdminProfileUpdateDTO;
import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.security.JwtUtils;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private TokenVerifierUtils tokenVerifierUtils;
    @Autowired private JwtUtils jwtUtils;

    // ================================
    // GET Admin Profile
    // ================================
    @GetMapping("/{adminId}")
    public ResponseEntity<ApiResponse<?>> getAdminProfile(
            @PathVariable Long adminId,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request for admin profile adminId={}", adminId);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                log.warn("Unauthorized request for admin profile adminId={}", adminId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            Optional<Admin> adminOpt = adminService.getActiveAdminById(adminId);
            if (adminOpt.isEmpty()) {
                log.warn("Admin not found adminId={}", adminId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Admin not found", null));
            }

            Admin admin = adminOpt.get();

            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, admin.getEmail(), false);
            if (authResponse != null) {
                log.warn("Email authorization failed for admin profile adminId={}", adminId);
                return authResponse;
            }

            Map<String, Object> response = Map.of(
                    "adminId", admin.getAdminId(),
                    "userId", admin.getUserId().toString(),
                    "email", admin.getEmail(),
                    "name", admin.getName(),
                    "status", admin.getStatus(),
                    "phoneNum", admin.getPhoneNum() != null ? admin.getPhoneNum() : "",
                    "birthDate", admin.getBirthDate() != null ? admin.getBirthDate() : "",
                    "registeredDate", admin.getRegisteredDate() != null ? admin.getRegisteredDate() : ""
            );

            log.info("Successfully fetched admin profile adminId={}", adminId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Admin profile fetched successfully", response));

        } catch (Exception e) {
            log.error("Error fetching admin profile adminId={}", adminId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    // ================================
    // UPDATE Admin Profile
    // ================================
    @PutMapping("/profile/{adminId}")
    public ResponseEntity<ApiResponse<?>> updateAdminProfile(
            @PathVariable Long adminId,
            @Valid @RequestBody AdminProfileUpdateDTO dto,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request to update admin profile adminId={}", adminId);

        try {
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                log.warn("Unauthorized request to update admin profile adminId={}", adminId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            Optional<Admin> adminOpt = adminService.getActiveAdminById(adminId);
            if (adminOpt.isEmpty()) {
                log.warn("Admin not found adminId={}", adminId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Admin not found", null));
            }

            Admin admin = adminOpt.get();

            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, admin.getEmail(), false);
            if (authResponse != null) {
                log.warn("Email authorization failed for update admin profile adminId={}", adminId);
                return authResponse;
            }

            Admin updatedAdmin = adminService.updateAdminProfile(adminId, dto);

            log.info("Successfully updated admin profile adminId={}", adminId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Admin profile updated successfully", updatedAdmin));

        } catch (Exception e) {
            log.error("Error updating admin profile adminId={}", adminId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }
}