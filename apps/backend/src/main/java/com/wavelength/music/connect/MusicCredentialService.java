package com.wavelength.music.connect;

import com.wavelength.common.ApiException;
import com.wavelength.music.MusicAccount;
import com.wavelength.music.MusicAccountRepository;
import com.wavelength.music.MusicProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MusicCredentialService {
    private final MusicAccountRepository accounts;
    private final MusicTokenCipher cipher;
    private final SpotifyGateway spotify;

    public MusicCredentialService(MusicAccountRepository accounts, MusicTokenCipher cipher, SpotifyGateway spotify) {
        this.accounts = accounts;
        this.cipher = cipher;
        this.spotify = spotify;
    }

    @Transactional
    public void saveSpotify(UUID userId, String providerUserId, SpotifyGateway.Tokens tokens) {
        var account = replaceOrReuse(userId, MusicProvider.SPOTIFY, providerUserId);
        account.setAccessTokenEncrypted(cipher.encrypt(account.getId(), MusicProvider.SPOTIFY, "access", tokens.accessToken()));
        account.setRefreshTokenEncrypted(cipher.encrypt(account.getId(), MusicProvider.SPOTIFY, "refresh", tokens.refreshToken()));
        account.setTokenExpiresAt(Instant.now().plusSeconds(tokens.expiresIn()));
        account.setUpdatedAt(Instant.now());
        accounts.saveAndFlush(account);
    }

    @Transactional
    public void saveApple(UUID userId, String tokenFingerprint, String musicUserToken) {
        var account = replaceOrReuse(userId, MusicProvider.APPLE_MUSIC, tokenFingerprint);
        account.setAccessTokenEncrypted(cipher.encrypt(account.getId(), MusicProvider.APPLE_MUSIC, "access", musicUserToken));
        account.setRefreshTokenEncrypted(null);
        account.setTokenExpiresAt(null);
        account.setUpdatedAt(Instant.now());
        accounts.saveAndFlush(account);
    }

    @Transactional
    public void disconnect(UUID userId, MusicProvider provider) {
        accounts.findByUserIdAndProvider(userId, provider).ifPresent(accounts::delete);
    }

    @Transactional
    public List<MusicAccount> listForUser(UUID userId) {
        var result = accounts.findAllByUserId(userId);
        if (cipher.configured()) result.forEach(this::rotateIfNeeded);
        return result;
    }

    @Transactional
    public void refreshSpotify(UUID userId) {
        accessTokenForSpotify(userId);
    }

    @Transactional
    public String accessTokenForSpotify(UUID userId) {
        var account = accounts.lockByUserIdAndProvider(userId, MusicProvider.SPOTIFY)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MUSIC_ACCOUNT_NOT_FOUND", "Spotify is not connected"));
        if (account.getAccessTokenEncrypted() == null || account.getRefreshTokenEncrypted() == null)
            throw new ApiException(HttpStatus.CONFLICT, "MUSIC_RECONNECT_REQUIRED", "Reconnect Spotify to continue");
        if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            var access = cipher.decrypt(account.getId(), MusicProvider.SPOTIFY, "access", account.getAccessTokenEncrypted());
            rotateIfNeeded(account);
            return access;
        }
        var refresh = cipher.decrypt(account.getId(), MusicProvider.SPOTIFY, "refresh", account.getRefreshTokenEncrypted());
        var tokens = spotify.refresh(refresh);
        account.setAccessTokenEncrypted(cipher.encrypt(account.getId(), MusicProvider.SPOTIFY, "access", tokens.accessToken()));
        account.setRefreshTokenEncrypted(cipher.encrypt(account.getId(), MusicProvider.SPOTIFY, "refresh",
                tokens.refreshToken().isBlank() ? refresh : tokens.refreshToken()));
        account.setTokenExpiresAt(Instant.now().plusSeconds(tokens.expiresIn()));
        account.setUpdatedAt(Instant.now());
        accounts.save(account);
        return tokens.accessToken();
    }

    private void rotateIfNeeded(MusicAccount account) {
        var accessEncrypted = account.getAccessTokenEncrypted();
        var refreshEncrypted = account.getRefreshTokenEncrypted();
        var rotateAccess = cipher.needsRotation(accessEncrypted);
        var rotateRefresh = cipher.needsRotation(refreshEncrypted);
        if (!rotateAccess && !rotateRefresh) return;
        if (rotateAccess) {
            var access = cipher.decrypt(account.getId(), account.getProvider(), "access", accessEncrypted);
            account.setAccessTokenEncrypted(cipher.encrypt(account.getId(), account.getProvider(), "access", access));
        }
        if (rotateRefresh) {
            var refresh = cipher.decrypt(account.getId(), account.getProvider(), "refresh", refreshEncrypted);
            account.setRefreshTokenEncrypted(cipher.encrypt(account.getId(), account.getProvider(), "refresh", refresh));
        }
        account.setUpdatedAt(Instant.now());
        accounts.save(account);
    }

    private MusicAccount replaceOrReuse(UUID userId, MusicProvider provider, String providerUserId) {
        var existing = accounts.findByUserIdAndProvider(userId, provider).orElse(null);
        if (existing != null && existing.getProviderUserId().equals(providerUserId)) return existing;
        if (existing != null) {
            accounts.delete(existing);
            accounts.flush();
        }
        return new MusicAccount(userId, provider, providerUserId);
    }
}
