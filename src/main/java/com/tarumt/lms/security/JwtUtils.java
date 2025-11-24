package com.tarumt.lms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${supabase.jwt.secret}")
    private String secret;

    public Map<String, Object> verifyToken(String token) {
        try {
            if (token.startsWith("Bearer ")) token = token.substring(7);
            Key key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims;
        } catch (Exception e) {
            return null; // let TokenVerifier decide response
        }
    }
}
