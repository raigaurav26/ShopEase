package com.shopease.auth.service;

import com.shopease.auth.config.JwtUtil;
import com.shopease.auth.dto.AuthResponse;
import com.shopease.auth.dto.LoginRequest;
import com.shopease.auth.dto.RegisterRequest;
import com.shopease.auth.exception.AuthException;
import com.shopease.auth.model.User;
import com.shopease.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, refreshTokenService,
                Clock.fixed(NOW, ZoneOffset.UTC), 5, Duration.ofMinutes(15));

        registerRequest = new RegisterRequest();
        registerRequest.setName("Gaurav Rai");
        registerRequest.setEmail("gaurav@test.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("gaurav@test.com");
        loginRequest.setPassword("password123");

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Gaurav Rai");
        savedUser.setEmail("gaurav@test.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole("CUSTOMER");
    }

    @Test
    void register_ShouldReturnTokens_WhenNewUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("test-jwt-token");
        when(refreshTokenService.issue(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(registerRequest);

        assertEquals("test-jwt-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldStoreEmailLowercased() {
        registerRequest.setEmail("  Gaurav@Test.COM ");
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.empty());

        authService.register(registerRequest);

        verify(userRepository).save(argThat(u -> u.getEmail().equals("gaurav@test.com")));
    }

    @Test
    void register_ShouldReturnConflict_WhenEmailExists() {
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.of(savedUser));

        AuthException ex = assertThrows(AuthException.class, () -> authService.register(registerRequest));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnTokens_WhenValidCredentials() {
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("test-jwt-token");
        when(refreshTokenService.issue(savedUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertEquals("test-jwt-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void login_ShouldGiveSameError_ForUnknownEmailAndWrongPassword() {
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.empty());
        AuthException unknown = assertThrows(AuthException.class, () -> authService.login(loginRequest));

        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);
        AuthException wrongPassword = assertThrows(AuthException.class, () -> authService.login(loginRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, unknown.getStatus());
        assertEquals(unknown.getStatus(), wrongPassword.getStatus());
        assertEquals(unknown.getMessage(), wrongPassword.getMessage());
    }

    @Test
    void login_ShouldStillCheckAPassword_WhenEmailUnknown() {
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> authService.login(loginRequest));

        verify(passwordEncoder).matches(eq("password123"), any());
    }

    @Test
    void login_ShouldCountFailedAttempts() {
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login(loginRequest));

        assertEquals(1, savedUser.getFailedLoginAttempts());
        assertNull(savedUser.getLockedUntil());
        verify(userRepository).save(savedUser);
    }

    @Test
    void login_ShouldLockAccount_AfterFiveFailures() {
        savedUser.setFailedLoginAttempts(4);
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login(loginRequest));

        assertEquals(NOW.plus(Duration.ofMinutes(15)), savedUser.getLockedUntil());
        assertEquals(0, savedUser.getFailedLoginAttempts());
    }

    @Test
    void login_ShouldRejectCorrectPassword_WhileLocked() {
        savedUser.setLockedUntil(NOW.plusSeconds(60));
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.of(savedUser));

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(loginRequest));

        assertEquals(HttpStatus.LOCKED, ex.getStatus());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void login_ShouldSucceedAndResetCounters_AfterLockExpires() {
        savedUser.setLockedUntil(NOW.minusSeconds(1));
        savedUser.setFailedLoginAttempts(2);
        when(userRepository.findByEmail("gaurav@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        authService.login(loginRequest);

        assertNull(savedUser.getLockedUntil());
        assertEquals(0, savedUser.getFailedLoginAttempts());
        verify(userRepository).save(savedUser);
    }

    @Test
    void refresh_ShouldReturnNewTokenPair() {
        when(refreshTokenService.rotate("old"))
                .thenReturn(new RefreshTokenService.Rotation(savedUser, "new"));
        when(jwtUtil.generateToken("gaurav@test.com", "CUSTOMER")).thenReturn("access");

        AuthResponse response = authService.refresh("old");

        assertEquals("access", response.getAccessToken());
        assertEquals("new", response.getRefreshToken());
    }
}
