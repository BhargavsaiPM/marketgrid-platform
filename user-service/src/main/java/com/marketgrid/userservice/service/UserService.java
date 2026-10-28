package com.marketgrid.userservice.service;

import com.marketgrid.userservice.entity.Role;
import com.marketgrid.userservice.entity.User;
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
     * Register a new user. The password is BCrypt-hashed before persistence.
     *
     * @throws IllegalArgumentException if the username or email already exists
     */
    @Transactional
    public User register(String username, String password, String email, Role role) {
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
     * Authenticate and return a signed JWT.
     *
     * @throws IllegalArgumentException if the username is not found or the password is wrong
     */
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    /**
     * Look up a user by username (used by the /me endpoint after JWT extraction).
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
