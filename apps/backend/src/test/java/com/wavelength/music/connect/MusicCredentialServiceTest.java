package com.wavelength.music.connect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wavelength.music.MusicAccount;
import com.wavelength.music.MusicAccountRepository;
import com.wavelength.music.MusicProvider;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MusicCredentialServiceTest {
    @Test
    void storesCiphertextAndKeepsOrRotatesSpotifyRefreshToken() {
        var key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        var cipher = new MusicTokenCipher("current:" + Base64.getEncoder().encodeToString(key));
        var accounts = mock(MusicAccountRepository.class);
        var gateway = mock(SpotifyGateway.class);
        var service = new MusicCredentialService(accounts, cipher, gateway);
        var userId = UUID.randomUUID();
        when(accounts.findByUserIdAndProvider(userId, MusicProvider.SPOTIFY)).thenReturn(Optional.empty());
        service.saveSpotify(userId, "spotify-id", new SpotifyGateway.Tokens("first", "original-refresh", 3600));
        var saved = org.mockito.ArgumentCaptor.forClass(MusicAccount.class);
        verify(accounts).saveAndFlush(saved.capture());
        var account = saved.getValue();
        assertNotEquals("first", account.getAccessTokenEncrypted());
        assertNotEquals("original-refresh", account.getRefreshTokenEncrypted());
        assertEquals("original-refresh", cipher.decrypt(account.getId(), MusicProvider.SPOTIFY,
                "refresh", account.getRefreshTokenEncrypted()));

        when(accounts.lockByUserIdAndProvider(userId, MusicProvider.SPOTIFY)).thenReturn(Optional.of(account));
        account.setTokenExpiresAt(Instant.EPOCH);
        when(gateway.refresh("original-refresh")).thenReturn(new SpotifyGateway.Tokens("second", "", 3600));
        assertEquals("second", service.accessTokenForSpotify(userId));
        assertEquals("original-refresh", cipher.decrypt(account.getId(), MusicProvider.SPOTIFY,
                "refresh", account.getRefreshTokenEncrypted()));

        account.setTokenExpiresAt(Instant.EPOCH);
        when(gateway.refresh("original-refresh")).thenReturn(new SpotifyGateway.Tokens("third", "rotated-refresh", 3600));
        assertEquals("third", service.accessTokenForSpotify(userId));
        assertEquals("rotated-refresh", cipher.decrypt(account.getId(), MusicProvider.SPOTIFY,
                "refresh", account.getRefreshTokenEncrypted()));
    }

    @Test
    void storesAppleUserTokenEncryptedWithoutInventingRefreshToken() {
        var key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        var cipher = new MusicTokenCipher("current:" + Base64.getEncoder().encodeToString(key));
        var accounts = mock(MusicAccountRepository.class);
        var service = new MusicCredentialService(accounts, cipher, mock(SpotifyGateway.class));
        var userId = UUID.randomUUID();
        when(accounts.findByUserIdAndProvider(userId, MusicProvider.APPLE_MUSIC)).thenReturn(Optional.empty());
        service.saveApple(userId, "token-fingerprint", "apple-user-token");
        var saved = org.mockito.ArgumentCaptor.forClass(MusicAccount.class);
        verify(accounts).saveAndFlush(saved.capture());
        var account = saved.getValue();
        assertEquals("apple-user-token", cipher.decrypt(account.getId(), MusicProvider.APPLE_MUSIC,
                "access", account.getAccessTokenEncrypted()));
        assertNull(account.getRefreshTokenEncrypted());
        assertNull(account.getTokenExpiresAt());
    }

    @Test
    void reencryptsAppleTokenWithNewKeyWhenAccountsAreListed() {
        var oldBytes = new byte[32];
        var newBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(oldBytes);
        new java.security.SecureRandom().nextBytes(newBytes);
        var oldKey = Base64.getEncoder().encodeToString(oldBytes);
        var newKey = Base64.getEncoder().encodeToString(newBytes);
        var userId = UUID.randomUUID();
        var account = new MusicAccount(userId, MusicProvider.APPLE_MUSIC, "fingerprint");
        account.setAccessTokenEncrypted(new MusicTokenCipher("old:" + oldKey)
                .encrypt(account.getId(), MusicProvider.APPLE_MUSIC, "access", "apple-token"));
        var accounts = mock(MusicAccountRepository.class);
        when(accounts.findAllByUserId(userId)).thenReturn(List.of(account));
        var cipher = new MusicTokenCipher("new:" + newKey + ",old:" + oldKey);
        var service = new MusicCredentialService(accounts, cipher, mock(SpotifyGateway.class));
        service.listForUser(userId);
        assertFalse(cipher.needsRotation(account.getAccessTokenEncrypted()));
        assertEquals("apple-token", cipher.decrypt(account.getId(), MusicProvider.APPLE_MUSIC,
                "access", account.getAccessTokenEncrypted()));
        verify(accounts).save(account);
    }
}
