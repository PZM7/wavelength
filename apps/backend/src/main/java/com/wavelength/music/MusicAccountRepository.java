package com.wavelength.music;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MusicAccountRepository extends JpaRepository<MusicAccount, UUID> {
    List<MusicAccount> findAllByUserId(UUID userId);
}
