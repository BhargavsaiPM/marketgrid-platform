package com.marketgrid.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for POST /api/users/login.
 */
public class LoginResponse {

    private String token;

    @Schema(description = "Account role: CUSTOMER, VENDOR, or ADMIN", example = "CUSTOMER")
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
