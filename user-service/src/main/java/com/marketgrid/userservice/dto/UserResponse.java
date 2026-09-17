package com.marketgrid.userservice.dto;

import com.marketgrid.userservice.entity.Role;
import com.marketgrid.userservice.entity.User;

/**
 * Safe projection of a User (excludes password).
 */
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    /** Convenience factory — strips the password from a User entity. */
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    // ---- Getters & Setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
