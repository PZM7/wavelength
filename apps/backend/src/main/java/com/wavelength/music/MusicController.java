package com.wavelength.music;

import com.wavelength.auth.CurrentUserProvider;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
public class MusicController {
    private final CurrentUserProvider current;
    private final MusicAccountRepository accounts;
    private final MusicDnaService dna;

    public MusicController(
            CurrentUserProvider current, MusicAccountRepository accounts, MusicDnaService dna) {
        this.current = current;
        this.accounts = accounts;
        this.dna = dna;
    }

    @GetMapping("/music-accounts")
    public List<MusicAccountResponse> accounts() {
        return accounts.findAllByUserId(current.get().getId()).stream()
                .map(
                        account ->
                                new MusicAccountResponse(
                                        account.getId(),
                                        account.getProvider(),
                                        account.getCreatedAt()))
                .toList();
    }

    @GetMapping("/music-dna")
    public MusicDna dna() {
        return dna.get(current.get().getId());
    }
}
