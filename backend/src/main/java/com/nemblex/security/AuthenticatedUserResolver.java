package com.nemblex.security;

import com.nemblex.entity.AppUser;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

    private final AppUserRepository userRepository;

    public AuthenticatedUserResolver(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser resolve(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public Long resolveId(Authentication authentication) {
        return resolve(authentication).getId();
    }
}
