package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.dto.StudentProfileUpdateDTO;
import com.tarumt.lms.model.Student;
import com.tarumt.lms.security.JwtUtils;
import com.tarumt.lms.service.user.StudentService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenVerifierUtils tokenVerifierUtils;


    // ================================
    // GET Student Profile
    // ================================
    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<?>> getStudentProfile(
            @PathVariable Long studentId,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request for student profile studentId={}", studentId);

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Fetch student
            Optional<Student> studentOpt = studentService.getActiveStudentById(studentId);
            if (studentOpt.isEmpty()) {
                log.warn("Student not found studentId={}", studentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Student not found", null));
            }

            Student student = studentOpt.get();

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, student.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("studentId", student.getStudentId());
            response.put("userId", student.getUserId() != null ? student.getUserId().toString() : "");
            response.put("email", student.getEmail() != null ? student.getEmail() : "");
            response.put("name", student.getName() != null ? student.getName() : "");
            response.put("status", student.getStatus() != null ? student.getStatus() : "Active");
            response.put("phoneNum", student.getPhoneNum() != null ? student.getPhoneNum() : "");
            response.put("birthDate", student.getBirthDate() != null ? student.getBirthDate() : "");
            response.put("registeredDate", student.getRegisteredDate() != null ? student.getRegisteredDate() : "");

            log.info("Returning student profile for studentId={}", studentId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Student profile fetched successfully", response));

        } catch (RuntimeException e) {
            log.error("Error fetching student profile studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error fetching student profile studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // UPDATE Student Profile
    // ================================
    @PutMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<?>> updateStudentProfile(
            @PathVariable("id") Long studentId,
            @Valid @RequestBody StudentProfileUpdateDTO dto,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request to update student profile. studentId={}, updateData={}", studentId, dto);

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Fetch student
            Optional<Student> studentOpt = studentService.getActiveStudentById(studentId);
            if (studentOpt.isEmpty()) {
                log.warn("Student not found studentId={}", studentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Student not found", null));
            }

            Student student = studentOpt.get();

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, student.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Update student
            Student updatedStudent = studentService.updateStudentProfile(studentId, dto);

            log.info("Successfully updated student profile: studentId={}", updatedStudent.getStudentId());

            return ResponseEntity.ok(new ApiResponse<>(true, "Student profile updated successfully", updatedStudent));

        } catch (RuntimeException e) {
            log.error("Runtime exception for studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error while updating studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }
}