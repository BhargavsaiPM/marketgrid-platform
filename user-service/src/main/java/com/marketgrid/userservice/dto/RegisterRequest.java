package com.marketgrid.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for POST /api/users/register.
 */
public class RegisterRequest {

    private String username;
    private String password;
    private String email;

    @Schema(description = "Account type: CUSTOMER or VENDOR (default CUSTOMER)", example = "CUSTOMER")
    private String accountType;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, String email) {
        this(username, password, email, "CUSTOMER");
    }

    public RegisterRequest(String username, String password, String email, String accountType) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.accountType = accountType;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
