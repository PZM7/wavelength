package com.wavelength.social;

import com.wavelength.auth.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conversations/{id}")
public class ChatController {
    private final CurrentUserProvider current;
    private final ChatService chat;

    public ChatController(CurrentUserProvider current, ChatService chat) {
        this.current = current;
        this.chat = chat;
    }

    @GetMapping
    public ChatConversation conversation(@PathVariable UUID id) {
        return chat.conversation(current.get().getId(), id);
    }

    @GetMapping("/messages")
    public ChatMessagePage messages(@PathVariable UUID id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) @Nullable Long before) {
        return chat.messages(current.get().getId(), id, limit, before);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessage send(@PathVariable UUID id, @Valid @RequestBody ChatMessageRequest request) {
        return chat.send(current.get().getId(), id, request);
    }
}
