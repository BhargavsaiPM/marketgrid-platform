package com.marketgrid.apigateway.filter;

import com.marketgrid.apigateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Global reactive filter for perimeter JWT validation at the API Gateway.
 *
 * <p><strong>Defense-in-Depth Architecture:</strong>
 * This filter enforces an edge-level security checkpoint. The API Gateway inspects incoming
 * HTTP requests and blocks obviously invalid, expired, or missing tokens early before they
 * can consume downstream bandwidth or microservice resources.
 * However, this is an <em>additional edge-layer check, not a replacement</em>: each downstream
 * microservice (user-service, vendor-service, product-service, order-service) independently
 * re-validates the token, inspects claims, and enforces its own fine-grained role-based
 * access controls (RBAC). If a service is called internally or if permissions differ, the
 * destination service remains fully self-protecting.</p>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // 1. Allow public endpoints to pass through untouched
        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        // 2. Read and validate Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 3. Verify signature and expiration using shared secret
        if (!jwtUtil.validateToken(token)) {
            return onError(exchange, "Invalid, expired, or tampered JWT token", HttpStatus.UNAUTHORIZED);
        }

        // 4. Token is valid: forward the request downstream as-is.
        // Downstream services will independently re-validate and extract user/role claims.
        return chain.filter(exchange);
    }

    /**
     * Determines whether the requested path and HTTP method are public and do not require a JWT.
     */
    private boolean isPublicPath(String path, HttpMethod method) {
        // Preflight CORS requests
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }

        // Authentication endpoints
        if (path.equals("/api/users/register") || path.equals("/api/users/login")) {
            return true;
        }

        // Public vendor browsing: GET /api/vendors and GET /api/vendors/{id}
        // Note: /api/vendors/me and /api/vendors/pending require authentication and are excluded here
        if (HttpMethod.GET.equals(method)) {
            if (path.equals("/api/vendors") || path.equals("/api/vendors/")) {
                return true;
            }
            if (path.startsWith("/api/vendors/")) {
                String sub = path.substring("/api/vendors/".length());
                if (sub.matches("\\d+") || (!sub.equalsIgnoreCase("me") && !sub.equalsIgnoreCase("pending") && !sub.contains("/"))) {
                    return true;
                }
            }
        }

        // Public product catalog browsing: GET /api/products/**
        if (HttpMethod.GET.equals(method) && (path.equals("/api/products") || path.startsWith("/api/products/"))) {
            return true;
        }

        // Actuator health and metrics endpoints
        if (path.startsWith("/actuator")) {
            return true;
        }

        // OpenAPI / Swagger UI endpoints and webjars assets across all services
        if (path.contains("/swagger-ui") || path.contains("/v3/api-docs") || path.contains("/webjars")) {
            return true;
        }

        return false;
    }

    /**
     * Writes a clean, structured JSON 401 Unauthorized response without forwarding downstream.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Execute early in the filter chain before downstream routing
        return -100;
    }
}
