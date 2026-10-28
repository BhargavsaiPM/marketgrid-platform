package com.marketgrid.userservice.service;

import com.marketgrid.userservice.dto.LoginResponse;
import com.marketgrid.userservice.entity.Role;
import com.marketgrid.userservice.entity.User;
import com.marketgrid.userservice.exception.PortalMismatchException;
import com.marketgrid.userservice.repository.UserRepository;
import com.marketgrid.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String TEST_SECRET = "bWFya2V0Z3JpZC1jb21tZXJjZS1zdXBlci1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tMjAyNg==";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtUtil jwtUtil;
    private UserService userService;
    private User sampleUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 86400000L);
        userService = new UserService(userRepository, passwordEncoder, jwtUtil);

        sampleUser = new User("alice", "hashed_pass", "alice@example.com", Role.CUSTOMER);
        sampleUser.setId(1L);

        adminUser = new User("admin", "hashed_admin_pass", "admin@example.com", Role.ADMIN);
        adminUser.setId(99L);
    }

    @Test
    void testRegister_Success() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain_pass")).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User created = userService.register("alice", "plain_pass", "alice@example.com", "CUSTOMER");

        assertNotNull(created);
        assertEquals("alice", created.getUsername());
        assertEquals(Role.CUSTOMER, created.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegister_AccountTypeAdmin_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("hacker", "pass", "hacker@example.com", "ADMIN"));

        assertTrue(ex.getMessage().contains("Self-registration as ADMIN is strictly forbidden"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_AccountTypeInvalid_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("user", "pass", "user@example.com", "SUPERUSER"));

        assertTrue(ex.getMessage().contains("Invalid accountType"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("alice", "plain_pass", "alice@example.com", "CUSTOMER"));

        assertTrue(ex.getMessage().contains("Username already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateEmail() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("bob", "plain_pass", "alice@example.com", "CUSTOMER"));

        assertTrue(ex.getMessage().contains("Email already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("plain_pass", "hashed_pass")).thenReturn(true);

        LoginResponse response = userService.login("alice", "plain_pass", "CUSTOMER");

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("CUSTOMER", response.getRole());
        assertTrue(jwtUtil.validateToken(response.getToken()));
        assertEquals("alice", jwtUtil.getClaims(response.getToken()).getSubject());
        assertEquals("CUSTOMER", jwtUtil.getClaims(response.getToken()).get("role", String.class));
    }

    @Test
    void testLogin_AdminPortalSuccess() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("admin_pass", "hashed_admin_pass")).thenReturn(true);

        LoginResponse response = userService.login("admin", "admin_pass", "ADMIN");

        assertNotNull(response);
        assertEquals("ADMIN", response.getRole());
        assertTrue(jwtUtil.validateToken(response.getToken()));
        assertEquals("ADMIN", jwtUtil.getClaims(response.getToken()).get("role", String.class));
    }

    @Test
    void testLogin_WrongPortal_ThrowsPortalMismatchException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser)); // Role CUSTOMER
        when(passwordEncoder.matches("plain_pass", "hashed_pass")).thenReturn(true);

        PortalMismatchException ex = assertThrows(PortalMismatchException.class, () ->
                userService.login("alice", "plain_pass", "VENDOR")); // Request VENDOR portal

        assertTrue(ex.getMessage().contains("does not match requested portal"));
    }

    @Test
    void testLogin_InvalidPassword() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "hashed_pass")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.login("alice", "wrong_pass", "CUSTOMER"));

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void testLogin_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.login("unknown", "any_pass", null));

        assertEquals("Invalid username or password", ex.getMessage());
    }
}
