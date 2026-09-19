package com.marketgrid.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String TEST_SECRET = "bWFya2V0Z3JpZC1jb21tZXJjZS1zdXBlci1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tMjAyNg==";
    private JwtUtil jwtUtil;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET);
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
    }

    @Test
    void testValidateToken_ValidToken() {
        String token = Jwts.builder()
                .subject("alice")
                .claim("role", "CUSTOMER")
                .claim("userId", 1L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        assertTrue(jwtUtil.validateToken(token));
        Claims claims = jwtUtil.getClaims(token);
        assertEquals("alice", claims.getSubject());
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        String token = Jwts.builder()
                .subject("alice")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(signingKey)
                .compact();

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_MalformedToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.structure"));
        assertFalse(jwtUtil.validateToken(""));
    }
}
