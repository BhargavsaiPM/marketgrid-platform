package com.marketgrid.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign client for user-service, resolved via Eureka.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * POST /api/users/login — obtain a JWT for credentials.
     */
    @PostMapping("/api/users/login")
    LoginResponse login(@RequestBody LoginRequest request);
}
