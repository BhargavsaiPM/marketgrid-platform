package com.marketgrid.userservice.controller;

import com.marketgrid.userservice.dto.*;
import com.marketgrid.userservice.entity.Role;
import com.marketgrid.userservice.entity.User;
import com.marketgrid.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user registration, authentication, and profile access.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/users/register
     * Public — creates a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot self-register as ADMIN"));
        }
        if (request.getRole() != Role.CUSTOMER && request.getRole() != Role.VENDOR) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role must be either CUSTOMER or VENDOR"));
        }

        try {
            User user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getRole()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(user));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/users/login
     * Public — authenticates and returns a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = userService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/users/me
     * Protected — returns the authenticated user's profile (JWT required).
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
