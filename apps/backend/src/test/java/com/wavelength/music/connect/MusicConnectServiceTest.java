package com.wavelength.music.connect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wavelength.common.ApiException;
import com.wavelength.music.MusicProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MusicConnectServiceTest {
    @Test
    void spotifyCallbackUsesSingleUseStateAndReturnsOnlyOutcome() {
        var settings = mock(MusicConnectSettings.class);
        var states = mock(MusicConnectStateStore.class);
        var spotify = mock(SpotifyGateway.class);
        var apple = mock(AppleMusicGateway.class);
        var credentials = mock(MusicCredentialService.class);
        var service = new MusicConnectService(settings, states, spotify, apple, credentials);
        var user = UUID.randomUUID();
        var pending = new MusicConnectStateStore.Pending(user, MusicProvider.SPOTIFY,
                "wavelength://music/callback", "verifier");
        when(states.consume("state")).thenReturn(Optional.of(pending)).thenReturn(Optional.empty());
        var tokens = new SpotifyGateway.Tokens("access-secret", "refresh-secret", 3600);
        when(spotify.exchange("code-secret", "verifier")).thenReturn(tokens);
        when(spotify.accountId("access-secret")).thenReturn("stable-id");
        var result = service.completeSpotify("state", "code-secret", null).toString();
        assertEquals("wavelength://music/callback?music=connected&provider=SPOTIFY", result);
        assertFalse(result.contains("code-secret"));
        assertFalse(result.contains("access-secret"));
        verify(credentials).saveSpotify(user, "stable-id", tokens);
        assertThrows(ApiException.class, () -> service.completeSpotify("state", "code-secret", null));
    }

    @Test
    void appleUserTokenIsVerifiedBeforeStateIsConsumedAndStored() {
        var settings = mock(MusicConnectSettings.class);
        var states = mock(MusicConnectStateStore.class);
        var spotify = mock(SpotifyGateway.class);
        var apple = mock(AppleMusicGateway.class);
        var credentials = mock(MusicCredentialService.class);
        var service = new MusicConnectService(settings, states, spotify, apple, credentials);
        var user = UUID.randomUUID();
        var pending = new MusicConnectStateStore.Pending(user, MusicProvider.APPLE_MUSIC,
                "http://localhost:8081/music/callback", "");
        when(states.peek("state")).thenReturn(Optional.of(pending));
        when(states.consume("state")).thenReturn(Optional.of(pending));
        when(apple.verifyUserToken("apple-secret")).thenReturn("token-fingerprint");
        var result = service.completeApple("state", "apple-secret").toString();
        assertEquals("http://localhost:8081/music/callback?music=connected&provider=APPLE_MUSIC", result);
        var order = inOrder(apple, states, credentials);
        order.verify(states).peek("state");
        order.verify(apple).verifyUserToken("apple-secret");
        order.verify(states).consume("state");
        order.verify(credentials).saveApple(user, "token-fingerprint", "apple-secret");
    }
}
