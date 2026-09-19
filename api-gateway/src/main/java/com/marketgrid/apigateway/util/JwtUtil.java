package com.marketgrid.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * JWT utility for API Gateway edge validation.
 *
 * <p>Validates and parses tokens issued by user-service using the shared HMAC signing secret.
 * The Gateway verifies token integrity, signature validity, and expiration at the perimeter
 * before routing downstream.</p>
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String base64Secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    /**
     * Parse and verify the token signature, returning its claims payload.
     *
     * @param token the compact JWT string
     * @return Claims payload
     * @throws io.jsonwebtoken.JwtException if the token is invalid, tampered with, or expired
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns {@code true} if the token is structurally valid, properly signed, and not expired.
     *
     * @param token the compact JWT string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
