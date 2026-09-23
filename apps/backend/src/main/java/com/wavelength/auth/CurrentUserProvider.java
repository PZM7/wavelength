package com.wavelength.auth;

import com.wavelength.common.ApiException;
import com.wavelength.users.User;
import com.wavelength.users.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    private final UserService users;

    public CurrentUserProvider(UserService users) {
        this.users = users;
    }

    public User get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        return users.resolve(jwt.getSubject());
    }
}
