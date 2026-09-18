package com.marketgrid.orderservice.security;

import com.marketgrid.orderservice.client.LoginRequest;
import com.marketgrid.orderservice.client.LoginResponse;
import com.marketgrid.orderservice.client.UserServiceClient;
import com.marketgrid.orderservice.exception.ServiceUnavailableException;
import com.marketgrid.orderservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Manages an internal service account JWT token used for service-to-service calls
 * requiring elevated permissions (e.g. stock decrements on product-service).
 *
 * <p>TODO: In production, replace this with a proper service-to-service auth mechanism
 * like OAuth2 client credentials, not a shared admin login.</p>
 */
@Component
public class ServiceAuthTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthTokenProvider.class);

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;

    @Value("${service.account.username:admin}")
    private String username;

    @Value("${service.account.password:Admin@12345}")
    private String password;

    private String cachedToken;
    private Date expiration;

    public ServiceAuthTokenProvider(UserServiceClient userServiceClient, JwtUtil jwtUtil) {
        this.userServiceClient = userServiceClient;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Retrieve a valid cached admin JWT token, refreshing it if expired or absent.
     */
    public synchronized String getAdminToken() {
        long now = System.currentTimeMillis();
        // Refresh if missing or expiring within 60 seconds
        if (cachedToken == null || expiration == null || expiration.before(new Date(now + 60_000))) {
            refreshToken();
        }
        return cachedToken;
    }

    private void refreshToken() {
        log.info("Authenticating internal service account '{}' with user-service...", username);
        try {
            LoginResponse response = userServiceClient.login(new LoginRequest(username, password));
            if (response == null || response.getToken() == null || response.getToken().isBlank()) {
                throw new ServiceUnavailableException("user-service returned empty token for service account");
            }
            this.cachedToken = response.getToken();
            Claims claims = jwtUtil.getClaims(cachedToken);
            this.expiration = claims.getExpiration();
            log.info("Service account token refreshed successfully. Expires at: {}", expiration);
        } catch (Exception e) {
            log.error("Failed to obtain service account token from user-service: {}", e.getMessage());
            throw new ServiceUnavailableException(
                    "Failed to authenticate service account with user-service. Ensure user-service is running.", e);
        }
    }
}
