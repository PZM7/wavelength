package com.wavelength.users;

public final class UserMapper {
    private UserMapper() {}

    public static UserProfile profile(User user) {
        return new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getCity(),
                user.getBirthDate(),
                user.isDiscoverable(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public static PublicUser publicProfile(User user) {
        return new PublicUser(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }
}
