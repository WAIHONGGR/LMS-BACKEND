package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.dto.RegisterRequest;
import com.tarumt.lms.model.*;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.model.enums.Role;
import com.tarumt.lms.security.JwtUtils;
import com.tarumt.lms.service.UserRoleService;
import com.tarumt.lms.service.user.AdminService;
import com.tarumt.lms.service.user.InstructorService;
import com.tarumt.lms.service.user.StudentService;
import com.tarumt.lms.service.user.SuperAdminService;
import com.tarumt.lms.utility.EmailUtils;
import com.tarumt.lms.utility.TokenVerifierUtils;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRoleService userRoleService;
    @Autowired private StudentService studentService;
    @Autowired private InstructorService instructorService;
    @Autowired private AdminService adminService;
    @Autowired private SuperAdminService superAdminService;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private TokenVerifierUtils tokenVerifierUtils;
    @Autowired private EmailUtils emailUtils;

    // ================================
    // REGISTER / LOGIN - STUDENT
    // ================================
    @PostMapping("/student/register")
    public ResponseEntity<ApiResponse<?>> registerStudent(
            @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Register student request received: email={}", request.getEmail());

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, request.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Check student email domain
            if (!emailUtils.isValidEmailDomain(request.getEmail(), "@student.tarc.edu.my")) {
                log.warn("Student registration blocked due to invalid email domain: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Invalid email domain for student registration", null));
            }


            Optional<Student> existingByEmail = studentService.getByEmail(request.getEmail());
            Optional<Student> existingByUserId = studentService.getByUserId(UUID.fromString(request.getSupabaseUserId()));



            Student user;

            if (existingByEmail.isPresent()) {
                user = existingByEmail.get();
                log.info("Existing student found by email: {}", request.getEmail());
            } else if (existingByUserId.isPresent()) {
                user = existingByUserId.get();
                log.info("Existing student found by userId: {}", request.getSupabaseUserId());
            } else {
                UUID supabaseUserId = UUID.fromString(request.getSupabaseUserId());
                user = (Student) userRoleService.registerUser(Role.STUDENT, request.getEmail(), request.getName(), supabaseUserId);
                log.info("New student registered: email={}, userId={}", request.getEmail(), supabaseUserId);
            }

            // If user is found but is INACTIVE, reject
            if (user.getStatus() == AccountStatus.INACTIVE) {
                log.warn("Attempted registration with inactive account: email={}, userId={}, studentId={}",
                        request.getEmail(), user.getUserId(), user.getStudentId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Your account is inactive. Please contact support.", null));
            }

            Map<String, Object> userData = Map.of(
                    "userId", user.getUserId(),
                    "studentId", user.getStudentId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", Role.STUDENT.name()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Student registered successfully", userData));

        } catch (RuntimeException e) {
            log.warn("Runtime exception during student registration: email={}, error={}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error during student registration: email={}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // REGISTER / LOGIN - INSTRUCTOR
    // ================================
    @PostMapping("/instructor/register")
    public ResponseEntity<ApiResponse<?>> registerInstructor(
            @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Register instructor request received: email={}", request.getEmail());

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, request.getEmail(), false);
            if (authResponse != null) return authResponse;


            // IMPORTANT: Check if email is already used by admin or superadmin FIRST
            // This check must happen before checking for existing instructor to prevent
            // admin/superadmin emails from logging in as instructors
            if (adminService.existsByEmail(request.getEmail())) {
                log.warn("Instructor registration/login blocked: email belongs to an Admin");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Email is already used.", null));
            }

            if (superAdminService.existsByEmail(request.getEmail())) {
                log.warn("Instructor registration/login blocked: email belongs to a SuperAdmin");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Email is already used.", null));
            }

            // Check instructor email domain
            if (!emailUtils.isValidEmailDomain(request.getEmail(), "@gmail.com")) {
                log.warn("Instructor registration blocked due to invalid email domain: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Invalid email domain for instructor registration", null));
            }


            Optional<Instructor> existingEmail = instructorService.getByEmail(request.getEmail());
            Optional<Instructor> existingByUserId = instructorService.getByUserId(UUID.fromString(request.getSupabaseUserId()));

            Instructor user;

            if (existingEmail.isPresent()) {
                user = existingEmail.get();
                log.info("Existing instructor found by email: {}", request.getEmail());
            } else if (existingByUserId.isPresent()) {
                user = existingByUserId.get();
                log.info("Existing instructor found by userId: {}", request.getSupabaseUserId());
            } else {
                UUID supabaseUserId = UUID.fromString(request.getSupabaseUserId());
                user = (Instructor) userRoleService.registerUser(Role.INSTRUCTOR, request.getEmail(), request.getName(), supabaseUserId);
                log.info("New instructor registered: email={}, userId={}", request.getEmail(), supabaseUserId);
            }

            if (user.getStatus() == AccountStatus.INACTIVE) {
                log.warn("Attempted registration with inactive account: email={}, userId={}, instructorId={}",
                        request.getEmail(), user.getUserId(), user.getInstructorId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Your account is inactive. Please contact support.", null));
            }

            Map<String, Object> userData = Map.of(
                    "userId", user.getUserId(),
                    "instructorId", user.getInstructorId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", Role.INSTRUCTOR.name()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Instructor registered successfully", userData));

        } catch (RuntimeException e) {
            log.warn("Runtime exception during instructor registration: email={}, error={}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error during instructor registration: email={}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // ADMIN LOGIN
    // ================================
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<?>> loginAdmin(
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Admin login request");

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid or expired token", null));
            }

            String emailFromToken = (String) claims.get("email");
            UUID userIdFromToken = UUID.fromString((String) claims.get("sub"));

            // Use AdminService to handle login logic
            Admin admin = adminService.handleAdminLogin(emailFromToken, userIdFromToken);

            Map<String, Object> userData = Map.of(
                    "userId", admin.getUserId().toString(),
                    "adminId", admin.getAdminId(),
                    "email", admin.getEmail(),
                    "name", admin.getName(),
                    "role", Role.ADMIN.name(),
                    "status", admin.getStatus()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Admin login successful", userData));

        } catch (IllegalStateException e) {
            log.warn("Admin login blocked: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error during admin login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // SUPER ADMIN LOGIN
    // ================================
    @PostMapping("/superadmin/login")
    public ResponseEntity<ApiResponse<?>> loginSuperAdmin(
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("SuperAdmin login request");

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Invalid or expired token", null));
            }

            String emailFromToken = (String) claims.get("email");
            UUID userIdFromToken = UUID.fromString((String) claims.get("sub"));

            // Retrieve SuperAdmin by email or userId
            Optional<SuperAdmin> existingByEmail = superAdminService.getByEmail(emailFromToken);
            Optional<SuperAdmin> existingByUserId = superAdminService.getByUserId(userIdFromToken);

            if (existingByEmail.isEmpty() && existingByUserId.isEmpty()) {
                log.warn("Unauthorized login attempt for non-existent superadmin: email={}", emailFromToken);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Access denied.", null));
            }

            SuperAdmin superAdmin = existingByEmail.orElse(existingByUserId.get());

            // Reject if inactive
            if (superAdmin.getStatus() == AccountStatus.INACTIVE) {
                log.warn("Attempted login with inactive SuperAdmin account: email={}, userId={}",
                        superAdmin.getEmail(), superAdmin.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Your account is inactive. Please contact support.", null));
            }

            Map<String, Object> userData = Map.of(
                    "userId", superAdmin.getUserId(),
                    "superAdminId", superAdmin.getSuperAdminId(),
                    "email", superAdmin.getEmail(),
                    "name", superAdmin.getName(),
                    "role", Role.SUPER_ADMIN.name(),
                    "status", superAdmin.getStatus()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "SuperAdmin login successful", userData));

        } catch (RuntimeException e) {
            log.warn("Runtime exception during SuperAdmin login: error={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid or expired token", null));
        } catch (Exception e) {
            log.error("Unexpected error during SuperAdmin login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // HEALTH CHECK
    // ================================
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        log.info("Health check endpoint called");
        return ResponseEntity.ok(new ApiResponse<>(true, "Server is running on port 8090!", "Server is running on port 8090!"));
    }

}