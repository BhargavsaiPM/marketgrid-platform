package com.marketgrid.userservice.service;

import com.marketgrid.userservice.dto.LoginResponse;
import com.marketgrid.userservice.entity.Role;
import com.marketgrid.userservice.entity.User;
import com.marketgrid.userservice.exception.PortalMismatchException;
import com.marketgrid.userservice.repository.UserRepository;
import com.marketgrid.userservice.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business logic for user registration and authentication.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user with role-safe accountType validation.
     *
     * @param accountType string indicating account type: CUSTOMER or VENDOR. Defaults to CUSTOMER if null/blank.
     * @throws IllegalArgumentException if accountType is ADMIN, invalid, or if username/email exists.
     */
    @Transactional
    public User register(String username, String password, String email, String accountType) {
        String type = (accountType == null || accountType.isBlank()) ? "CUSTOMER" : accountType.trim().toUpperCase();

        if ("ADMIN".equals(type)) {
            throw new IllegalArgumentException("Self-registration as ADMIN is strictly forbidden");
        }

        Role role;
        try {
            role = Role.valueOf(type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid accountType: " + accountType + ". Must be CUSTOMER or VENDOR");
        }

        if (role != Role.CUSTOMER && role != Role.VENDOR) {
            throw new IllegalArgumentException("Only CUSTOMER or VENDOR accounts can be registered");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User user = new User(username, passwordEncoder.encode(password), email, role);
        return userRepository.save(user);
    }

    /**
     * Authenticate and return LoginResponse with JWT and role.
     *
     * <p>Password is verified FIRST. Portal mismatch is checked ONLY after valid credentials.</p>
     *
     * @param portal optional portal hint (CUSTOMER, VENDOR, ADMIN)
     * @throws IllegalArgumentException if username not found or password incorrect
     * @throws PortalMismatchException if portal hint does not match account role
     */
    public LoginResponse login(String username, String password, String portal) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // 1. Verify password FIRST
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // 2. Portal-aware check after password verification
        if (portal != null && !portal.isBlank()) {
            String requestedPortal = portal.trim().toUpperCase();
            if (!user.getRole().name().equals(requestedPortal)) {
                throw new PortalMismatchException(
                        "Access denied: account role (" + user.getRole().name() +
                        ") does not match requested portal (" + requestedPortal + ")");
            }
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getRole().name());
    }

    /**
     * Look up a user by username.
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
