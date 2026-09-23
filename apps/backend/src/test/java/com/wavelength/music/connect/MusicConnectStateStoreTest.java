package com.wavelength.music.connect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavelength.music.MusicProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class MusicConnectStateStoreTest {
    @Test
    void statesExpireAndCanBeConsumedOnlyOnce() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") var values = (ValueOperations<String, String>) mock(ValueOperations.class);
        var data = new HashMap<String, String>();
        when(redis.opsForValue()).thenReturn(values);
        doAnswer(call -> { data.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(values).set(anyString(), anyString(), eq(Duration.ofMinutes(10)));
        when(values.get(anyString())).thenAnswer(call -> data.get(call.getArgument(0)));
        when(values.getAndDelete(anyString())).thenAnswer(call -> data.remove(call.getArgument(0)));
        var store = new MusicConnectStateStore(redis, new ObjectMapper());
        var pending = new MusicConnectStateStore.Pending(UUID.randomUUID(), MusicProvider.SPOTIFY,
                "wavelength://music/callback", "verifier");
        var state = store.create(pending);
        assertEquals(pending, store.peek(state).orElseThrow());
        assertEquals(pending, store.consume(state).orElseThrow());
        assertTrue(store.consume(state).isEmpty());
        assertTrue(store.peek("invalid").isEmpty());
        assertTrue(data.isEmpty());
        verify(values).set(argThat(key -> !key.contains(state)), anyString(), eq(Duration.ofMinutes(10)));
    }
}
