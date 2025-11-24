package com.tarumt.lms.controller;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.model.Student;
import com.tarumt.lms.model.enums.AccountStatus;
import com.tarumt.lms.repo.StudentRepository;
import com.tarumt.lms.service.AIChatService;
import com.tarumt.lms.utility.TokenVerifierUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService chatService;
    private final TokenVerifierUtils tokenVerifierUtils;
    private final StudentRepository studentRepository;

    // =====================================================
    // CHAT ENDPOINT - Send message and get AI response
    // =====================================================
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<?>> sendMessage(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> request) {

        try {
            // Validate JWT token and extract claims
            Map<String, Object> claims = tokenVerifierUtils.validateTokenAndGetClaims(authorizationHeader, true);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized user", null));
            }

            // Extract email from token claims
            String email = (String) claims.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, "Unauthorized user", null));
            }

            // Verify that the user is an active student
            Optional<Student> studentOpt = studentRepository.findByEmail(email);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Unauthorized user", null));
            }

            Student student = studentOpt.get();

            // Check if student account is active
            if (student.getStatus() == null || student.getStatus() != AccountStatus.ACTIVE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Unauthorized user", null));
            }

            // Extract message from request
            String userMessage = request.get("message");
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(false, "Message cannot be empty", null));
            }

            // Use student ID and role from the authenticated student
            String userId = String.valueOf(student.getStudentId());
            String userRole = "STUDENT";

            log.info("Chat request received from student: studentId={}, email={}, message length={}",
                    student.getStudentId(), email, userMessage.length());

            // Get AI response with database context
            String aiResponse = chatService.getChatResponse(userMessage, userId, userRole);

            return ResponseEntity.ok(new ApiResponse<>(true, "Chat response generated",
                    Map.of("response", aiResponse)));

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error processing chat message: " + e.getMessage(), null));
        }
    }

    // =====================================================
    // HEALTH CHECK - Test LLM connection
    // =====================================================
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> healthCheck() {
        try {
            boolean isHealthy = chatService.checkHealth();
            return ResponseEntity.ok(new ApiResponse<>(isHealthy,
                    isHealthy ? "Chat service is healthy" : "Chat service is unavailable", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(false, "Chat service error: " + e.getMessage(), null));
        }
    }
}

