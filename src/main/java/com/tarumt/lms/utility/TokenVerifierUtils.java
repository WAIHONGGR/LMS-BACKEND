package com.tarumt.lms.utility;

import com.tarumt.lms.dto.ApiResponse;
import com.tarumt.lms.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TokenVerifierUtils {

    @Autowired
    private JwtUtils jwtUtils;

    // ===============================
    // Stage 1: Validate the token and return claims
    // ===============================
    public Map<String, Object> validateTokenAndGetClaims(String authorizationHeader, boolean debug) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null; // Invalid format
        }

        Map<String, Object> claims = jwtUtils.verifyToken(authorizationHeader);
        if (claims == null || !claims.containsKey("email")) {
            return null; // Invalid or expired token
        }

        return claims; // Return claims for further use
    }

    // ===============================
    // Stage 1: Validate the token (backward compatibility)
    // ===============================
    public ResponseEntity<ApiResponse<?>> validateToken(String authorizationHeader, boolean debug) {
        Map<String, Object> claims = validateTokenAndGetClaims(authorizationHeader, debug);
        if (claims == null) {
            String msg = debug ? "Invalid or expired token" : "Unauthorized";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, msg, null));
        }
        return null; // token is valid
    }

    // ===============================
    // Stage 2: Authorize email from token
    // ===============================
    public ResponseEntity<ApiResponse<?>> authorizeEmail(Map<String, Object> claims, String emailToMatch, boolean debug) {
        String emailFromToken = (String) claims.get("email");
        if (!emailFromToken.equalsIgnoreCase(emailToMatch)) {
            String msg = debug ? "Token email mismatch" : "Unauthorized";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, msg, null));
        }
        return null; // email matches
    }

//     ===============================
//     Convenience method (optional - you can keep it commented or remove it)
//     ===============================
     public ResponseEntity<ApiResponse<?>> verify(String authorizationHeader, String emailToMatch, boolean debug) {
         Map<String, Object> claims = validateTokenAndGetClaims(authorizationHeader, debug);
         if (claims == null) {
             String msg = debug ? "Invalid or expired token" : "Unauthorized";
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                     .body(new ApiResponse<>(false, msg, null));
         }
         return authorizeEmail(claims, emailToMatch, debug);
     }
}