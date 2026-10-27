package com.shopease.auth.service;

import com.shopease.auth.exception.AuthException;
import com.shopease.auth.model.RefreshToken;
import com.shopease.auth.model.User;
import com.shopease.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(7));
        user = new User("Gaurav Rai", "gaurav@test.com", "hash", "CUSTOMER");
        user.setId(1L);
    }

    @Test
    void issue_ShouldStoreOnlyTheHash() {
        String raw = service.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertNotEquals(raw, saved.getValue().getTokenHash());
        assertEquals(RefreshTokenService.hash(raw), saved.getValue().getTokenHash());
        assertEquals(NOW.plus(Duration.ofDays(7)), saved.getValue().getExpiresAt());
    }

    @Test
    void issue_ShouldReturnDifferentTokensEachTime() {
        assertNotEquals(service.issue(user), service.issue(user));
    }

    @Test
    void rotate_ShouldRevokeOldTokenAndKeepFamily() {
        RefreshToken current = new RefreshToken(user, RefreshTokenService.hash("old"), "family-1", NOW.plusSeconds(60));
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("old"))).thenReturn(Optional.of(current));

        RefreshTokenService.Rotation rotation = service.rotate("old");

        assertTrue(current.isRevoked());
        assertSame(user, rotation.user());
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertEquals("family-1", saved.getValue().getFamilyId());
        assertEquals(RefreshTokenService.hash(rotation.refreshToken()), saved.getValue().getTokenHash());
    }

    @Test
    void rotate_ShouldRevokeAllSessions_WhenRevokedTokenIsReused() {
        RefreshToken stolen = new RefreshToken(user, RefreshTokenService.hash("old"), "family-1", NOW.plusSeconds(60));
        stolen.setRevoked(true);
        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(stolen));

        assertThrows(AuthException.class, () -> service.rotate("old"));

        verify(repository).revokeAllForUser(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_ShouldRejectExpiredToken() {
        RefreshToken expired = new RefreshToken(user, RefreshTokenService.hash("old"), "family-1", NOW);
        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(expired));

        assertThrows(AuthException.class, () -> service.rotate("old"));
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_ShouldRejectUnknownToken() {
        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> service.rotate("nope"));
    }

    @Test
    void revokeSession_ShouldRevokeTheWholeFamily() {
        RefreshToken token = new RefreshToken(user, RefreshTokenService.hash("t"), "family-1", NOW.plusSeconds(60));
        when(repository.findByTokenHash(RefreshTokenService.hash("t"))).thenReturn(Optional.of(token));

        service.revokeSession("t");

        verify(repository).revokeFamily("family-1");
    }
}
