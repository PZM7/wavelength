package com.wavelength.music;

import com.wavelength.auth.CurrentUserProvider;
import com.wavelength.music.connect.MusicCredentialService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
public class MusicController {
    private final CurrentUserProvider current;
    private final MusicCredentialService credentials;
    private final MusicDnaService dna;
    private final TopTrackRepository topTracks;

    public MusicController(
            CurrentUserProvider current, MusicCredentialService credentials, MusicDnaService dna,
            TopTrackRepository topTracks) {
        this.current = current;
        this.credentials = credentials;
        this.dna = dna;
        this.topTracks = topTracks;
    }

    @GetMapping("/music-accounts")
    public List<MusicAccountResponse> accounts() {
        return credentials.listForUser(current.get().getId()).stream()
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

    @GetMapping("/top-tracks")
    public List<TopTrack> topTracks() {
        return topTracks.forUser(current.get().getId());
    }
}
