package com.nemblex.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nemblex.service.interfaces.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationManagerTest {

    @Mock
    private UserService userService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private CustomAuthenticationManager authenticationManager;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authenticationManager = new CustomAuthenticationManager(userService, passwordEncoder);
    }

    @Test
    void authenticate_shouldThrowGenericBadCredentials_whenUserNotFound() {
        // Arrange
        when(userService.loadUserByUsername("ghost@nemblex.dev"))
                .thenThrow(new UsernameNotFoundException("No user with email: ghost@nemblex.dev"));
        Authentication request = new UsernamePasswordAuthenticationToken("ghost@nemblex.dev", "whatever");

        // Act & Assert
        assertThatThrownBy(() -> authenticationManager.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Correo electrónico o contraseña incorrectos");
    }

    @Test
    void authenticate_shouldThrowGenericBadCredentials_whenPasswordWrong() {
        // Arrange
        UserDetails userDetails = User.withUsername("ana@nemblex.dev")
                .password(passwordEncoder.encode("correct-password"))
                .authorities("ROLE_TECHNICIAN")
                .build();
        when(userService.loadUserByUsername("ana@nemblex.dev")).thenReturn(userDetails);
        Authentication request = new UsernamePasswordAuthenticationToken("ana@nemblex.dev", "wrong-password");

        // Act & Assert
        assertThatThrownBy(() -> authenticationManager.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Correo electrónico o contraseña incorrectos");
    }

    @Test
    void authenticate_shouldReturnAuthenticatedToken_whenCredentialsValid() {
        // Arrange
        UserDetails userDetails = User.withUsername("ana@nemblex.dev")
                .password(passwordEncoder.encode("correct-password"))
                .authorities("ROLE_TECHNICIAN")
                .build();
        when(userService.loadUserByUsername("ana@nemblex.dev")).thenReturn(userDetails);
        Authentication request = new UsernamePasswordAuthenticationToken("ana@nemblex.dev", "correct-password");

        // Act
        Authentication result = authenticationManager.authenticate(request);

        // Assert
        assertThat(result.getName()).isEqualTo("ana@nemblex.dev");
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    void authenticate_shouldKeepTheUserDetailsAsPrincipal_soDownstreamCodeCanAccessTheDomainUser() {
        // Arrange
        UserDetails userDetails = User.withUsername("ana@nemblex.dev")
                .password(passwordEncoder.encode("correct-password"))
                .authorities("ROLE_TECHNICIAN")
                .build();
        when(userService.loadUserByUsername("ana@nemblex.dev")).thenReturn(userDetails);
        Authentication request = new UsernamePasswordAuthenticationToken("ana@nemblex.dev", "correct-password");

        // Act
        Authentication result = authenticationManager.authenticate(request);

        // Assert
        assertThat(result.getPrincipal()).isSameAs(userDetails);
    }
}
