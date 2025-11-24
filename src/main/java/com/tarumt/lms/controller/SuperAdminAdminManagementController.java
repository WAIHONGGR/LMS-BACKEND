package com.tarumt.lms.controller.superadmin;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.model.Admin;
import com.tarumt.lms.model.SuperAdmin;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.model.enums.Role;
import com.tarumt.lms.security.JwtUtils;
import com.tarumt.lms.service.UserRoleService;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.service.user.InstructorService;
import com.tarumt.lms.service.user.StudentService;
import com.tarumt.lms.service.user.SuperAdminService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/superadmin/admins")
@CrossOrigin(origins = "*")
public class SuperAdminAdminManagementController {

    @Autowired private TokenVerifierUtils tokenVerifierUtils;
    @Autowired private SuperAdminService superAdminService;
    @Autowired private AdminService adminService;
    @Autowired private UserRoleService userRoleService;
    @Autowired private StudentService studentService;
    @Autowired private InstructorService instructorService;

    // =====================================================
// CREATE ADMIN (SuperAdmin Only) - FIXED VERSION
// =====================================================
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> request) {

        log.info("SuperAdmin request: Create admin email={}", request.get("email"));

        try {
            // Validate SuperAdmin
            Optional<SuperAdmin> superAdminOpt = validateSuperAdmin(authorizationHeader);

            if (superAdminOpt.isEmpty()) {
                log.warn("Unauthorized SuperAdmin access attempt");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "SuperAdmin privileges required", null));
            }

            // Extract request data
            String email = request.get("email");
            String name = request.get("name");

            // Validate required fields
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Email is required", null));
            }

            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Name is required", null));
            }

            // Check if email is already used by any role
            if (adminService.existsByEmail(email)) {
                log.warn("Admin creation blocked: email already exists as admin: {}", email);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(false, "Email is already used by an admin", null));
            }

            if (instructorService.existsByEmail(email)) {
                log.warn("Admin creation blocked: email already exists as instructor: {}", email);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(false, "Email is already used by an instructor", null));
            }

            if (superAdminService.existsByEmail(email)) {
                log.warn("Admin creation blocked: email already exists as superadmin: {}", email);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(false, "Email is already used by a superadmin", null));
            }

            // IMPORTANT: Create admin WITHOUT supabaseUserId initially
            // The userId will be set when the admin logs in with Google/Supabase
            // We'll use adminService.create() directly instead of UserRoleService
            // to avoid requiring a UUID

            Admin newAdmin = adminService.create(email, name, null); // Pass null for supabaseUserId

            // Note: We don't create UserRole entry here because there's no userId yet
            // The UserRole will be created when the admin logs in with Google/Supabase

            log.info("Successfully created admin: adminId={}, email={}, status={}",
                    newAdmin.getAdminId(), email, newAdmin.getStatus());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("adminId", newAdmin.getAdminId());
            response.put("userId", newAdmin.getUserId() != null ? newAdmin.getUserId().toString() : null);
            response.put("email", newAdmin.getEmail());
            response.put("name", newAdmin.getName());
            response.put("status", newAdmin.getStatus());
            response.put("registeredDate", newAdmin.getRegisteredDate());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Admin created successfully", response));

        } catch (RuntimeException e) {
            log.error("Error creating admin: email={}, error={}", request.get("email"), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error creating admin: email={}", request.get("email"), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // =====================================================
    // GET ALL ADMINS (OPTIONAL STATUS FILTER)
    // =====================================================
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllAdmins(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String status) {

        log.info("SuperAdmin request: Fetch admins (status={})", status);

        try {
            Optional<SuperAdmin> superAdminOpt = validateSuperAdmin(authorizationHeader);
            if (superAdminOpt.isEmpty()) {
                log.warn("Unauthorized SuperAdmin access attempt");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "SuperAdmin privileges required", null));
            }

            AccountStatus filter = null;
            if (status != null) {
                try {
                    filter = AccountStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, "Invalid status filter", null));
                }
            }

            List<Admin> admins = (filter == null)
                    ? adminService.getAllAdmins()
                    : adminService.getByStatus(filter);

            log.info("Fetched {} admins (status={})", admins.size(), status);

            return ResponseEntity.ok(new ApiResponse<>(true, "Admins fetched successfully", admins));

        } catch (Exception e) {
            log.error("Error fetching admins", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // =====================================================
    // GET ADMIN DETAILS BY ID (SuperAdmin View)
    // =====================================================
    @GetMapping("/{adminId}")
    public ResponseEntity<ApiResponse<?>> getAdminDetails(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long adminId) {

        log.info("SuperAdmin request: Get admin details adminId={}", adminId);

        try {
            Optional<SuperAdmin> superAdminOpt = validateSuperAdmin(authorizationHeader);

            if (superAdminOpt.isEmpty()) {
                log.warn("Unauthorized SuperAdmin access attempt");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "SuperAdmin privileges required", null));
            }

            Optional<Admin> adminOpt = adminService.getById(adminId);

            if (adminOpt.isEmpty()) {
                log.warn("Admin not found adminId={}", adminId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Admin not found", null));
            }

            Admin admin = adminOpt.get();

            // Build response with all admin details
            Map<String, Object> response = new HashMap<>();
            response.put("adminId", admin.getAdminId());
            response.put("userId", admin.getUserId().toString());
            response.put("email", admin.getEmail());
            response.put("name", admin.getName());
            response.put("status", admin.getStatus());
            response.put("phoneNum", admin.getPhoneNum() != null ? admin.getPhoneNum() : "");
            response.put("birthDate", admin.getBirthDate() != null ? admin.getBirthDate() : "");
            response.put("registeredDate", admin.getRegisteredDate() != null ? admin.getRegisteredDate() : "");
            response.put("endDate", admin.getEndDate() != null ? admin.getEndDate() : "");

            log.info("Successfully fetched admin details adminId={}", adminId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Admin details fetched successfully", response));

        } catch (Exception e) {
            log.error("Error fetching admin details adminId={}", adminId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // =====================================================
    // UPDATE ADMIN STATUS (ACTIVE / INACTIVE)
    // =====================================================
    @PutMapping("/{adminId}/status")
    public ResponseEntity<ApiResponse<?>> updateAdminStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long adminId,
            @RequestBody Map<String, String> body) {

        log.info("SuperAdmin request: Update Admin Status adminId={}", adminId);

        try {
            Optional<SuperAdmin> superAdminOpt = validateSuperAdmin(authorizationHeader);
            if (superAdminOpt.isEmpty()) {
                log.warn("Unauthorized SuperAdmin access attempt");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "SuperAdmin privileges required", null));
            }

            SuperAdmin superAdmin = superAdminOpt.get();

            // Get status and reason from request
            String statusStr = body.get("status");
            String reason = body.get("reason"); // Optional field for auditing

            if (statusStr == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Status is required", null));
            }

            // Convert string to enum safely
            AccountStatus newStatus;
            try {
                newStatus = AccountStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid status value", null));
            }

            // Only allow ACTIVE or INACTIVE
            if (newStatus != AccountStatus.ACTIVE && newStatus != AccountStatus.INACTIVE) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid status. Must be ACTIVE or INACTIVE", null));
            }

            // Call service and pass reason + superAdmin
            Admin updated = adminService.updateAdminStatus(adminId, newStatus, superAdmin, reason);

            return ResponseEntity.ok(new ApiResponse<>(true, "Admin status updated", updated));

        } catch (RuntimeException e) {
            log.error("Error updating admin status adminId={}", adminId, e);
            // Handle specific business logic exceptions (e.g., Admin not found)
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                log.warn("Admin not found: adminId={}, error={}", adminId, e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, e.getMessage(), null));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage() != null ? e.getMessage() : "Failed to update admin status", null));
        } catch (Exception e) {
            log.error("Unexpected error updating admin status adminId={}", adminId, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    // =====================================================
    // HELPER: Validate SuperAdmin
    // =====================================================
    private Optional<SuperAdmin> validateSuperAdmin(String authorizationHeader) {
        Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
        if (claims == null) return Optional.empty();

        UUID userId = UUID.fromString((String) claims.get("sub"));
        return superAdminService.getActiveByUserId(userId);
    }

}
