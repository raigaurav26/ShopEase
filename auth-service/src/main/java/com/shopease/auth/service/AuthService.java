package com.shopease.auth.service;

import com.shopease.auth.config.JwtUtil;
import com.shopease.auth.dto.AuthResponse;
import com.shopease.auth.dto.LoginRequest;
import com.shopease.auth.dto.RegisterRequest;
import com.shopease.auth.exception.AuthException;
import com.shopease.auth.model.User;
import com.shopease.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;
    private final int maxFailedAttempts;
    private final Duration lockDuration;

    // Compared against when the email is unknown, so a missing account takes as long as a wrong password
    private volatile String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService,
                       Clock clock,
                       @Value("${auth.lockout.max-attempts:5}") int maxFailedAttempts,
                       @Value("${auth.lockout.duration:15m}") Duration lockDuration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = lockDuration;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.getEmail());
        if (userRepository.findByEmail(email).isPresent()) {
            throw AuthException.emailTaken();
        }

        User user = new User();
        user.setName(req.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("CUSTOMER");

        userRepository.save(user);

        return tokensFor(user, refreshTokenService.issue(user), "User registered successfully");
    }

    // Failed attempts must be saved even though the call ends in an AuthException
    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalize(req.getEmail())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(req.getPassword(), dummyPasswordHash());
            throw AuthException.invalidCredentials();
        }

        Instant now = clock.instant();
        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            throw AuthException.accountLocked();
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            recordFailedAttempt(user, now);
            throw AuthException.invalidCredentials();
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return tokensFor(user, refreshTokenService.issue(user), "Login successful");
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        return tokensFor(rotation.user(), rotation.refreshToken(), "Token refreshed");
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeSession(refreshToken);
    }

    private void recordFailedAttempt(User user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= maxFailedAttempts) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(now.plus(lockDuration));
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepository.save(user);
    }

    private AuthResponse tokensFor(User user, String refreshToken, String message) {
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(accessToken, refreshToken, jwtUtil.getAccessTokenTtlSeconds(), message);
    }

    private String dummyPasswordHash() {
        if (dummyPasswordHash == null) {
            dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
        }
        return dummyPasswordHash;
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
