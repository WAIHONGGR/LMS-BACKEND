package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.dto.InstructorProfileUpdateDTO;
import com.tarumt.lms.dto.InstructorRequirementDTO;
import com.tarumt.lms.dto.InstructorRequirementStatusDTO;
import com.tarumt.lms.model.Instructor;
import com.tarumt.lms.model.InstructorQualification;
import com.tarumt.lms.security.JwtUtils;
import com.tarumt.lms.service.user.InstructorService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/instructor")
public class InstructorController {

    @Autowired
    private InstructorService instructorService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenVerifierUtils tokenVerifierUtils;


    // ================================
    // GET Instructor Profile
    // ================================
    @GetMapping("/{instructorId}")
    public ResponseEntity<ApiResponse<?>> getInstructorProfile(
            @PathVariable Long instructorId,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request for instructor profile information instructorId={}", instructorId);

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Fetch instructor
            Optional<Instructor> instructorOpt = instructorService.getActiveOrPendingById(instructorId);
            if (instructorOpt.isEmpty()) {
                log.warn("Instructor not found instructorId={}", instructorId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Instructor not found", null));
            }

            Instructor instructor = instructorOpt.get();

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, instructor.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Return profile only after validation and authorization
            Map<String, Object> response = new HashMap<>();
            response.put("instructorId", instructor.getInstructorId());
            response.put("userId", instructor.getUserId() != null ? instructor.getUserId().toString() : "");
            response.put("email", instructor.getEmail() != null ? instructor.getEmail() : "");
            response.put("name", instructor.getName() != null ? instructor.getName() : "");
            response.put("birthDate", instructor.getBirthDate() != null ? instructor.getBirthDate() : "");
            response.put("phoneNum", instructor.getPhoneNum() != null ? instructor.getPhoneNum() : "");
            response.put("status", instructor.getStatus() != null ? instructor.getStatus() : "Active");
            response.put("registeredDate", instructor.getRegisteredDate() != null ? instructor.getRegisteredDate() : "");

            log.info("Returning instructor profile for instructorId={}", instructorId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Instructor profile fetched successfully", response));

        } catch (RuntimeException e) {
            log.error("Error fetching instructor profile instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error fetching instructor profile instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // ================================
    // UPDATE Instructor Profile
    // ================================
    @PutMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<?>> updateInstructorProfile(
            @PathVariable("id") Long instructorId,
            @Valid @RequestBody InstructorProfileUpdateDTO dto,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request to update instructor profile. instructorId={}, updateData={}", instructorId, dto);

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Fetch instructor
            Optional<Instructor> instructorOpt = instructorService.getActiveOrPendingById(instructorId);
            if (instructorOpt.isEmpty()) {
                log.warn("Instructor not found instructorId={}", instructorId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Instructor not found", null));
            }

            Instructor instructor = instructorOpt.get();

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, instructor.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Update profile
            Instructor updatedInstructor = instructorService.updateInstructorProfile(instructorId, dto);

            log.info("Successfully updated instructor profile: instructorId={}", updatedInstructor.getInstructorId());

            return ResponseEntity.ok(new ApiResponse<>(true, "Instructor profile updated successfully", updatedInstructor));

        } catch (RuntimeException e) {
            log.error("Runtime exception for instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error while updating instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // =================================
    // GET Instructor Requirement Status
    // =================================
    @GetMapping("requirement/status/{instructorId}")
    public ResponseEntity<ApiResponse<?>> getRequirementsStatus(
            @PathVariable Long instructorId,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Received request for instructor requirements qualification status instructorId={}", instructorId);

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Fetch instructor
            Optional<Instructor> instructorOpt = instructorService.getActiveOrPendingById(instructorId);
            if (instructorOpt.isEmpty()) {
                log.warn("Instructor not found instructorId={}", instructorId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Instructor not found", null));
            }

            Instructor instructor = instructorOpt.get();

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, instructor.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Only after both checks pass, fetch requirement status
            InstructorRequirementStatusDTO statusDTO = instructorService.getRequirementsStatus(instructorId);

            log.info("Returning information for instructor requirements instructorId={}", instructorId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Instructor requirements status fetched", statusDTO));

        } catch (RuntimeException e) {
            log.error("Error fetching instructor requirements instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error fetching instructor requirements instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }


    // =================================
    // GET Submission History
    // =================================
    @GetMapping("/requirements/{instructorId}/history")
    public ResponseEntity<ApiResponse<?>> getSubmissionHistory(
            @PathVariable Long instructorId,
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("Received request for instructor submission history instructorId={}", instructorId);
        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Fetch instructor
            Optional<Instructor> instructorOpt = instructorService.getActiveOrPendingById(instructorId);
            if (instructorOpt.isEmpty()) {
                log.warn("Instructor not found instructorId={}", instructorId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Instructor not found", null));
            }

            Instructor instructor = instructorOpt.get();

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, instructor.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Get submission history from service
            List<InstructorQualification> qualifications = instructorService.getSubmissionHistory(instructorId);

            // Map to response DTOs
            List<Map<String, Object>> history = qualifications.stream().map(qual -> {
                Map<String, Object> qualMap = new HashMap<>();
                qualMap.put("qualificationId", qual.getId());
                qualMap.put("qualificationLevel", qual.getQualificationLevel());
                qualMap.put("qualificationType", qual.getQualificationLevel());
                qualMap.put("fieldOfStudy", qual.getFieldOfStudy());
                qualMap.put("status", qual.getStatus());
                qualMap.put("certificateDocument", qual.getCertificateDocument());
                qualMap.put("documentUrl", qual.getCertificateDocument());

                // Extract filename from URL for display
                String documentUrl = qual.getCertificateDocument();
                String fileName = extractFileNameFromUrl(documentUrl);
                qualMap.put("fileName", fileName);
                qualMap.put("documentName", fileName); // Alias for frontend compatibility

                qualMap.put("rejectionReason", qual.getRejectionReason());
                qualMap.put("submittedAt", qual.getSubmittedAt());
                qualMap.put("submittedDate", qual.getSubmittedAt());
                return qualMap;
            }).collect(Collectors.toList());

            log.info("Returning submission history for instructorId={}, count={}", instructorId, history.size());
            return ResponseEntity.ok(new ApiResponse<>(true, "Submission history fetched successfully", history));

        } catch (RuntimeException e) {
            log.error("Error fetching submission history instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error fetching submission history instructorId={}", instructorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }

    /**
     * Extract the filename from a Supabase Storage URL
     *
     * Examples:
     * Input:  "https://xxx.supabase.co/storage/v1/object/public/Instructor-Docs/certificates/instructor_24_1763353693367_Chapter 1.pdf"
     * Output: "Chapter 1.pdf"
     *
     * Input:  "certificates/instructor_24_1763353693367_Chapter 1.pdf"
     * Output: "Chapter 1.pdf"
     *
     * Input:  "certificates/DELETED_instructor_24_1763353693367_Chapter 1.pdf"
     * Output: "Chapter 1.pdf" (removes DELETED_ prefix and instructor prefix)
     *
     * @param urlOrPath The full URL or relative path
     * @return The clean filename (e.g., "Chapter 1.pdf") or null if extraction fails
     */
    private String extractFileNameFromUrl(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank() || urlOrPath.equals("DELETED")) {
            return null;
        }

        try {
            // Get the last part after the last '/'
            int lastSlashIndex = urlOrPath.lastIndexOf('/');
            String filename = lastSlashIndex >= 0 && lastSlashIndex < urlOrPath.length() - 1
                    ? urlOrPath.substring(lastSlashIndex + 1)
                    : urlOrPath;

            // Remove query parameters if any (e.g., ?token=...)
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf("?"));
            }

            // Remove hash fragments if any
            if (filename.contains("#")) {
                filename = filename.substring(0, filename.indexOf("#"));
            }

            // If filename is blank, return null
            if (filename.isBlank()) {
                return null;
            }

            // Extract the actual document name (remove instructor prefix pattern)
            // Pattern: instructor_{id}_{timestamp}_{actualFileName}
            // Example: instructor_24_1763353693367_Chapter 1.pdf -> Chapter 1.pdf
            if (filename.startsWith("instructor_")) {
                // Find the pattern: instructor_ followed by digits, underscore, digits, underscore
                // Then extract everything after the second underscore
                String[] parts = filename.split("_", 4); // Split into max 4 parts
                if (parts.length >= 4) {
                    // parts[0] = "instructor"
                    // parts[1] = instructor ID
                    // parts[2] = timestamp
                    // parts[3] = actual filename
                    filename = parts[3];
                } else if (parts.length == 3) {
                    // Fallback: if only 3 parts, use the last part
                    filename = parts[2];
                }
            }

            return filename;

        } catch (Exception e) {
            log.error("Error extracting filename from URL: {}", urlOrPath, e);
            return null;
        }
    }


    // ================================
    // SUBMIT Instructor Requirements
    // ================================
    @PutMapping("/requirements/{id}")
    public ResponseEntity<ApiResponse<?>> submitRequirements(
            @PathVariable Long id,
            @ModelAttribute InstructorRequirementDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        log.info("Received instructor requirement submission request. instructorId={}", id);

        try {
            // Validate token and get claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized", null));
            }

            // Fetch instructor
            Optional<Instructor> instructorOpt = instructorService.getActiveOrPendingById(id);
            if (instructorOpt.isEmpty()) {
                log.warn("Instructor not found for id={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Instructor not found", null));
            }

            Instructor instructor = instructorOpt.get();

            // Authorize email
            ResponseEntity<ApiResponse<?>> authResponse = tokenVerifierUtils.authorizeEmail(claims, instructor.getEmail(), false);
            if (authResponse != null) return authResponse;

            // Submit requirements
            Instructor updatedInstructor = instructorService.submitRequirements(id, dto);

            log.info("Requirements submitted successfully for instructorId={}", id);

            Map<String, Object> response = Map.of(
                    "digitalSignatureUrl", updatedInstructor.getDigitalSignature()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Requirements submitted successfully", response));

        } catch (RuntimeException e) {
            log.warn("Validation/runtime error for instructorId={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error while submitting requirements for instructorId={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error", null));
        }
    }
}