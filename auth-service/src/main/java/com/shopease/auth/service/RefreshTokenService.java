package com.shopease.auth.service;

import com.shopease.auth.exception.AuthException;
import com.shopease.auth.model.RefreshToken;
import com.shopease.auth.model.User;
import com.shopease.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues and rotates opaque refresh tokens.
 *
 * Every refresh revokes the presented token and returns a new one. If a token
 * that was already rotated is presented again, someone is holding a copy of it,
 * so every session of that user is revoked.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               Clock clock,
                               @Value("${jwt.refresh-token-ttl:7d}") Duration refreshTokenTtl) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public record Rotation(User user, String refreshToken) {
    }

    /** Starts a new session for the user and returns the raw token to hand to the client. */
    @Transactional
    public String issue(User user) {
        return issue(user, UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = AuthException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(AuthException::invalidRefreshToken);

        if (current.isRevoked()) {
            User user = current.getUser();
            int revoked = refreshTokenRepository.revokeAllForUser(user.getId());
            log.warn("Refresh token reuse detected for user {}; revoked {} active session token(s)",
                    user.getId(), revoked);
            throw AuthException.invalidRefreshToken();
        }
        if (!current.getExpiresAt().isAfter(clock.instant())) {
            throw AuthException.invalidRefreshToken();
        }

        current.setRevoked(true);
        String next = issue(current.getUser(), current.getFamilyId());
        return new Rotation(current.getUser(), next);
    }

    /** Ends the session the token belongs to. Unknown tokens are ignored so logout is idempotent. */
    @Transactional
    public void revokeSession(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId()));
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    @Scheduled(cron = "${jwt.refresh-token-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpired() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("Deleted {} expired refresh token(s)", deleted);
        }
    }

    private String issue(User user, String familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = clock.instant().plus(refreshTokenTtl);
        refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), familyId, expiresAt));
        return rawToken;
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
