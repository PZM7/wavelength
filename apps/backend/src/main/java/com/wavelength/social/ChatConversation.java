package com.wavelength.social;

import com.wavelength.users.PublicUser;
import java.util.UUID;

public record ChatConversation(UUID id, PublicUser otherUser) {}
