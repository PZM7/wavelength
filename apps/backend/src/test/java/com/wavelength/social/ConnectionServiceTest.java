package com.wavelength.social;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

class ConnectionServiceTest {
    @Test
    void selfRequestFailsBeforeTouchingDatabase() {
        var privacy = mock(PrivacyService.class);
        var service =
                new ConnectionService(
                        mock(ConnectionRepository.class), privacy, mock(JdbcTemplate.class));
        var id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.request(id, id));
        verifyNoInteractions(privacy);
    }

    @Test
    void listIsBounded() {
        var service =
                new ConnectionService(
                        mock(ConnectionRepository.class),
                        mock(PrivacyService.class),
                        mock(JdbcTemplate.class));
        assertThrows(
                IllegalArgumentException.class, () -> service.list(UUID.randomUUID(), 0, null));
    }
}
