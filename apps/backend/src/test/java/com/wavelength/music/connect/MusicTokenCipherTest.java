package com.wavelength.music.connect;

import static org.junit.jupiter.api.Assertions.*;

import com.wavelength.music.MusicProvider;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MusicTokenCipherTest {
    @Test
    void encryptsWithRandomNonceAndBindsTokenToAccountProviderAndPurpose() {
        var key = Base64.getEncoder().encodeToString(new byte[32]);
        var cipher = new MusicTokenCipher("current:" + key);
        var account = UUID.randomUUID();
        var first = cipher.encrypt(account, MusicProvider.SPOTIFY, "refresh", "secret-token");
        var second = cipher.encrypt(account, MusicProvider.SPOTIFY, "refresh", "secret-token");
        assertNotEquals(first, second);
        assertFalse(first.contains("secret-token"));
        assertEquals("secret-token", cipher.decrypt(account, MusicProvider.SPOTIFY, "refresh", first));
        assertThrows(IllegalStateException.class,
                () -> cipher.decrypt(UUID.randomUUID(), MusicProvider.SPOTIFY, "refresh", first));
        assertThrows(IllegalStateException.class,
                () -> cipher.decrypt(account, MusicProvider.APPLE_MUSIC, "refresh", first));
        assertThrows(IllegalStateException.class,
                () -> cipher.decrypt(account, MusicProvider.SPOTIFY, "access", first));
    }

    @Test
    void keepsOldKeyForDecryptionWhileNewWritesUseCurrentKey() {
        var oldKey = Base64.getEncoder().encodeToString(new byte[32]);
        var newKeyBytes = new byte[32];
        newKeyBytes[0] = 1;
        var newKey = Base64.getEncoder().encodeToString(newKeyBytes);
        var account = UUID.randomUUID();
        var oldValue = new MusicTokenCipher("old:" + oldKey)
                .encrypt(account, MusicProvider.SPOTIFY, "access", "old-token");
        var rotated = new MusicTokenCipher("new:" + newKey + ",old:" + oldKey);
        assertTrue(rotated.needsRotation(oldValue));
        assertEquals("old-token", rotated.decrypt(account, MusicProvider.SPOTIFY, "access", oldValue));
        var newValue = rotated.encrypt(account, MusicProvider.SPOTIFY, "access", "old-token");
        assertFalse(rotated.needsRotation(newValue));
        assertThrows(IllegalStateException.class,
                () -> new MusicTokenCipher("new:" + newKey).decrypt(account, MusicProvider.SPOTIFY, "access", oldValue));
    }
}
