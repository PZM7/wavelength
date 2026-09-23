package com.wavelength.users;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavelength.common.ApiException;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

class UserServiceTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final UserRepository repository = mock(UserRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserService service =
            new UserService(repository, jdbc, FACTORY.getValidator(), false);
    private final ObjectMapper mapper = new ObjectMapper();

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    @Test
    void existingSubjectResolvesInternalUser() {
        var user = new User("issuer-sub");
        when(repository.findByExternalAuthId("issuer-sub")).thenReturn(Optional.of(user));
        assertEquals(user.getId(), service.resolve("issuer-sub").getId());
    }

    @Test
    void disabledProvisioningRefusesUnknownSubject() {
        when(repository.findByExternalAuthId(anyString())).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.resolve("new"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void patchRejectsImmutableFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.update(UUID.randomUUID(), mapper.readTree("{\"id\":\"other\"}")));
    }

    @Test
    void patchPreservesOmissionsAndClearsExplicitNull() throws Exception {
        var user = new User("sub");
        user.setUsername("marc");
        user.setCity("Madrid");
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(user)).thenReturn(user);
        var result = service.update(user.getId(), mapper.readTree("{\"city\":null}"));
        assertNull(result.city());
        assertEquals("marc", result.username());
    }

    @Test
    void invalidUsernameFailsValidation() {
        var user = new User("sub");
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.update(user.getId(), mapper.readTree("{\"username\":\"A!\"}")));
    }
}
