package com.wavelength.music.connect;

import com.wavelength.music.MusicProvider;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MusicTokenCipher {
    private final Map<String, SecretKeySpec> keys;
    private final String currentKeyId;
    private final SecureRandom random = new SecureRandom();

    // MUSIC_TOKEN_KEYS=k2:<base64-32-byte-key>,k1:<previous-key>
    public MusicTokenCipher(@Value("${wavelength.music.token-keys:}") String encodedKeys) {
        var parsed = new LinkedHashMap<String, SecretKeySpec>();
        for (var entry : encodedKeys.split(",")) {
            if (entry.isBlank()) continue;
            var fields = entry.trim().split(":", 2);
            if (fields.length != 2 || !fields[0].matches("[A-Za-z0-9_-]{1,24}"))
                throw new IllegalArgumentException("Invalid music token key configuration");
            byte[] raw;
            try { raw = Base64.getDecoder().decode(fields[1]); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid music token key configuration"); }
            if (raw.length != 32 || parsed.putIfAbsent(fields[0], new SecretKeySpec(raw, "AES")) != null)
                throw new IllegalArgumentException("Invalid music token key configuration");
        }
        keys = Map.copyOf(parsed);
        currentKeyId = parsed.isEmpty() ? null : new ArrayList<>(parsed.keySet()).getFirst();
    }

    public boolean configured() { return currentKeyId != null; }

    public String encrypt(UUID accountId, MusicProvider provider, String purpose, String plaintext) {
        if (!configured() || plaintext == null || plaintext.isBlank())
            throw new IllegalStateException("Music token encryption is unavailable");
        var nonce = new byte[12];
        random.nextBytes(nonce);
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(currentKeyId), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(accountId, provider, purpose));
            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            var combined = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);
            return "v1:" + currentKeyId + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Music token encryption failed"); }
    }

    public String decrypt(UUID accountId, MusicProvider provider, String purpose, String ciphertext) {
        try {
            var parts = ciphertext.split(":", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw new IllegalArgumentException();
            var key = keys.get(parts[1]);
            if (key == null) throw new IllegalArgumentException();
            var combined = Base64.getUrlDecoder().decode(parts[2]);
            if (combined.length < 29) throw new IllegalArgumentException();
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, combined, 0, 12));
            cipher.updateAAD(aad(accountId, provider, purpose));
            return new String(cipher.doFinal(combined, 12, combined.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Music token decryption failed");
        }
    }

    public boolean needsRotation(String ciphertext) {
        return ciphertext != null && !ciphertext.startsWith("v1:" + currentKeyId + ":");
    }

    private static byte[] aad(UUID accountId, MusicProvider provider, String purpose) {
        return (accountId + "|" + provider.name() + "|" + purpose).getBytes(StandardCharsets.UTF_8);
    }
}
