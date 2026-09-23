package com.wavelength.music;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MusicAccountRepository extends JpaRepository<MusicAccount, UUID> {
    List<MusicAccount> findAllByUserId(UUID userId);
    Optional<MusicAccount> findByUserIdAndProvider(UUID userId, MusicProvider provider);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MusicAccount a where a.userId = :userId and a.provider = :provider")
    Optional<MusicAccount> lockByUserIdAndProvider(UUID userId, MusicProvider provider);
}
