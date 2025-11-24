package com.tarumt.lms.service;

import com.tarumt.lms.service.DatabaseContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Chat Service - Configured for Groq (Free, Fast, OpenAI-compatible)
 *
 * To use Groq (Recommended - Easiest):
 * 1. Get API key from https://console.groq.com/
 * 2. Add to application.properties:
 *    llm.api.url=https://api.groq.com/openai/v1/chat/completions
 *    llm.api.key=gsk-your-key-here
 *    llm.model=llama-3-8b-8192
 *    llm.enabled=true
 *
 * This service works with Groq without any code changes!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final DatabaseContextService databaseContextService;
    private final RestTemplate restTemplate;

    @Value("${llm.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    @Value("${llm.model:llama-3-8b-8192}")
    private String llmModel;

    @Value("${llm.enabled:false}")
    private boolean llmEnabled;

    /**
     * Get AI chat response with database context
     */
    public String getChatResponse(String userMessage, String userId, String userRole) {
        if (!llmEnabled) {
            return "AI chat service is currently disabled. Please contact your administrator.";
        }

        try {
            // Get database context (schema info, relevant data)
            String dbContext = databaseContextService.getDatabaseContext(userMessage, userId, userRole);

            // Build system prompt with database context
            String systemPrompt = buildSystemPrompt(dbContext, userRole);

            // Prepare messages for LLM
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmModel);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

            // Build messages array
            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();

            // System message with context
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // User message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            // Call LLM API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + llmApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling LLM API: {}", llmApiUrl);

            ResponseEntity<Map> response = restTemplate.exchange(
                    llmApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Extract response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                java.util.List<Map<String, Object>> choices = (java.util.List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }

            return "I apologize, but I couldn't generate a response. Please try again.";

        } catch (Exception e) {
            log.error("Error calling LLM API", e);
            return "I'm experiencing technical difficulties. Please try again later or contact support.";
        }
    }

    /**
     * Build system prompt with database context
     * Focused on course discovery, enrollment readiness, and recommendations.
     */
    private String buildSystemPrompt(String dbContext, String userRole) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are the TARUMT LMS Course Advisor and Learning Assistant.\n");
        prompt.append("You serve TWO purposes:\n\n");

        prompt.append("=== ROLE 1: TARUMT LMS COURSE ADVISOR ===\n");
        prompt.append("Use the provided DATABASE CONTEXT to answer questions about:\n");
        prompt.append("- available courses\n");
        prompt.append("- categories\n");
        prompt.append("- instructors\n");
        prompt.append("- enrollment guidance\n");
        prompt.append("- student's enrolled courses (if STUDENT ENROLLMENT INFORMATION is provided)\n");
        prompt.append("- course progress and status (if STUDENT ENROLLMENT INFORMATION is provided)\n");
        prompt.append("ALWAYS use DB context for LMS-related questions.\n");
        prompt.append("If a course is not in the context, say it is not available.\n");
        prompt.append("If STUDENT ENROLLMENT INFORMATION is provided, use it to answer questions about the student's specific enrollments.\n\n");

        prompt.append("=== ROLE 2: GENERAL LEARNING ASSISTANT ===\n");
        prompt.append("If the user asks about general topics (e.g., Java, Spring Boot, machine learning, coding help, definitions),\n");
        prompt.append("answer normally using your own general knowledge.\n");
        prompt.append("DO NOT force DB context when the question is not about LMS data.\n\n");

        prompt.append("=== DATABASE CONTEXT ===\n");
        prompt.append(dbContext);
        prompt.append("\n\n");

        prompt.append("=== RESPONSE RULES ===\n");
        prompt.append("- FIRST, decide if the question is LMS-related or a general learning question.\n");
        prompt.append("- For LMS questions → use the database context.\n");
        prompt.append("- For general learning questions → use your own knowledge.\n");
        prompt.append("- If the context lacks information, say so politely.\n");
        prompt.append("- Use clear, friendly language.\n");
        prompt.append("- Format answers with bullet points or short paragraphs.\n");
        prompt.append("\n=== CRITICAL: COURSE LISTING FORMAT ===\n");
        prompt.append("When displaying courses from the context, you MUST preserve the exact formatting:\n");
        prompt.append("- Each course should be on separate lines with proper line breaks\n");
        prompt.append("- Format: COURSE: [Name] on first line, then Category and Instructor on separate indented lines\n");
        prompt.append("- DO NOT combine all information into a single line\n");
        prompt.append("- DO NOT use pipe separators (|)\n");
        prompt.append("- PRESERVE line breaks and spacing for readability\n");
        prompt.append("- Example format:\n");
        prompt.append("  COURSE: Course Name\n");
        prompt.append("    - Category: Category Name\n");
        prompt.append("    - Instructor: Instructor Name\n\n");

        if (userRole != null && "student".equalsIgnoreCase(userRole)) {
            prompt.append("The user is a student. Provide supportive, encouraging guidance.\n");
        }

        return prompt.toString();
    }


    /**
     * Check if LLM service is healthy
     */
    public boolean checkHealth() {
        if (!llmEnabled) {
            return false;
        }
        try {
            // Simple test call
            String testResponse = getChatResponse("Hello", null, null);
            return testResponse != null && !testResponse.isEmpty();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return false;
        }
    }
}

