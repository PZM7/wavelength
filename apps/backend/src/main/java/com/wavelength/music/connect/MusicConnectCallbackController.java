package com.wavelength.music.connect;

import java.net.URI;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MusicConnectCallbackController {
    private final MusicConnectService connect;

    public record AppleComplete(String state, String musicUserToken) {}

    public MusicConnectCallbackController(MusicConnectService connect) { this.connect = connect; }

    @GetMapping("/api/v1/music/spotify/callback")
    public ResponseEntity<Void> spotify(@RequestParam String state,
            @RequestParam(required = false) String code, @RequestParam(required = false) String error) {
        return redirect(connect.completeSpotify(state, code, error));
    }

    @GetMapping(value = "/music/apple/connect", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> applePage(@RequestParam String state) {
        var page = connect.applePage(state);
        // Both inserted values are base64url/JWT strings. Attribute encoding still
        // prevents accidental markup interpretation if their format changes.
        var html = """
                <!doctype html><html lang="es"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Conectar Apple Music · Wavelength</title>
                <script defer src="https://js-cdn.music.apple.com/musickit/v3/musickit.js"></script>
                <script defer src="/music/apple-connect.js"></script>
                <style>body{font-family:system-ui;background:#141117;color:#f8f5fb;max-width:32rem;margin:4rem auto;padding:1rem}
                button{padding:1rem;border:0;border-radius:12px;background:#b451d6;color:white;font-weight:700;cursor:pointer}
                p{color:#bcb4c2;line-height:1.5}</style></head><body>
                <main id="connect" data-state="%s" data-token="%s">
                <h1>Conectar Apple Music</h1><p>Autoriza a Wavelength a leer tu biblioteca y actividad musical.</p>
                <button id="authorize" type="button">Autorizar con Apple Music</button>
                <p id="message" role="status"></p></main></body></html>
                """.formatted(attribute(page.state()), attribute(page.developerToken()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer")
                .header("X-Frame-Options", "DENY")
                .header("Content-Security-Policy", "frame-ancestors 'none'; base-uri 'none'; object-src 'none'")
                .contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping(value = "/api/v1/music/apple/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> appleComplete(@RequestBody AppleComplete body) {
        URI returnUri = connect.completeApple(body.state(), body.musicUserToken());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("returnUri", returnUri.toString()));
    }

    private static ResponseEntity<Void> redirect(URI uri) {
        return ResponseEntity.status(HttpStatus.FOUND).location(uri).cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").build();
    }

    private static String attribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("'", "&#39;");
    }
}
