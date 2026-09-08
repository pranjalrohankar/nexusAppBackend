package com.nexus.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${nexus.jwt.secret}")
    private String secret;

    @Value("${nexus.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getKey() {
        // amazonq-ignore-next-line
        // amazonq-ignore-next-line
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String role) {
        return generateToken(email, role, expirationMs);
    }

    public String generateToken(String email, String role, long customExpirationMs) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + customExpirationMs))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parser().verifyWith(getKey()).build()
                    .parseSignedClaims(token.trim()).getPayload().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return (String) Jwts.parser().verifyWith(getKey()).build()
                    .parseSignedClaims(token.trim()).getPayload().get("role");
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
