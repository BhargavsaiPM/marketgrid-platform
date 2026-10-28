package com.marketgrid.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for POST /api/users/login.
 */
public class LoginRequest {

    private String username;
    private String password;

    @Schema(description = "Optional portal hint: CUSTOMER, VENDOR, or ADMIN", example = "CUSTOMER")
    private String portal;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this(username, password, null);
    }

    public LoginRequest(String username, String password, String portal) {
        this.username = username;
        this.password = password;
        this.portal = portal;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }
}
