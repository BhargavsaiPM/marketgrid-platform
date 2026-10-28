package com.marketgrid.apigateway.filter;

import com.marketgrid.apigateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "bWFya2V0Z3JpZC1jb21tZXJjZS1zdXBlci1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tMjAyNg==";
    private JwtAuthenticationFilter filter;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET);
        filter = new JwtAuthenticationFilter(jwtUtil);
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
    }

    private String generateValidToken() {
        return Jwts.builder()
                .subject("alice")
                .claim("role", "CUSTOMER")
                .claim("userId", 1L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void testPublicPaths_RegisterPassesWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/users/register").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = filterExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled.get());
    }

    @Test
    void testPublicPaths_GetProductsPassesWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = filterExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled.get());
    }

    @Test
    void testPublicPaths_SwaggerAndApiDocsPassWithoutToken() {
        String[] publicDocPaths = {
                "/swagger-ui.html",
                "/swagger-ui/index.html",
                "/webjars/swagger-ui/swagger-ui.css",
                "/v3/api-docs/swagger-config",
                "/api/users/v3/api-docs",
                "/api/vendors/v3/api-docs",
                "/api/products/v3/api-docs",
                "/api/orders/v3/api-docs"
        };

        for (String path : publicDocPaths) {
            MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            AtomicBoolean chainCalled = new AtomicBoolean(false);
            GatewayFilterChain chain = filterExchange -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertTrue(chainCalled.get(), "Expected path to pass through without token: " + path);
        }
    }

    @Test
    void testProtectedPath_MissingTokenReturns401() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/orders/checkout").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = filterExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testProtectedPath_InvalidTokenReturns401() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/orders/checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = filterExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testProtectedPath_ValidTokenPassesDownstream() {
        String token = generateValidToken();
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/orders/checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = filterExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled.get());
    }
}
