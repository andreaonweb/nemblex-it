package com.nemblex.security;

import com.nemblex.service.interfaces.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationManager implements AuthenticationManager {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomAuthenticationManager(UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserDetails userDetails;
        try {
            userDetails = userService.loadUserByUsername(authentication.getName());
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(authentication.getCredentials().toString(), userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return new UsernamePasswordAuthenticationToken(
                authentication.getName(),
                userDetails.getPassword(),
                userDetails.getAuthorities());
    }
}
