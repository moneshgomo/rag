package com.gomo.rag.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility component for generating and validating JWT tokens.
 * Uses JJWT 0.12.x API with HS256 signing.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey  = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT containing the user's email and role as claims.
     *
     * @param email user's email (used as the subject)
     * @param role  user's role string (e.g. "MANAGER")
     * @return signed JWT string
     */
    public String generateToken(String email, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the email (subject) from a valid token.
     * Returns null if the token is invalid or expired.
     */
    public String extractEmail(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            log.debug("[JwtUtil] Failed to extract email: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the role claim from a valid token.
     * Returns null if the token is invalid or expired.
     */
    public String extractRole(String token) {
        try {
            return parseClaims(token).get("role", String.class);
        } catch (Exception e) {
            log.debug("[JwtUtil] Failed to extract role: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns true if the token is structurally valid, signed correctly, and not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("[JwtUtil] Token invalid: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}