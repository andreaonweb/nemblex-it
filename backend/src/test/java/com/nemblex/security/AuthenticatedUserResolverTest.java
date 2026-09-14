package com.nemblex.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nemblex.entity.AppUser;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private Authentication authentication;

    private AuthenticatedUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthenticatedUserResolver(userRepository);
    }

    @Test
    void resolve_shouldReturnUser_whenEmailMatches() {
        AppUser user = AppUser.builder().id(9L).email("carlos.mendez@nemblex.dev").build();
        when(authentication.getName()).thenReturn("carlos.mendez@nemblex.dev");
        when(userRepository.findByEmail("carlos.mendez@nemblex.dev")).thenReturn(Optional.of(user));

        assertThat(resolver.resolve(authentication)).isEqualTo(user);
    }

    @Test
    void resolve_shouldThrowResourceNotFoundException_whenNoUserMatchesEmail() {
        when(authentication.getName()).thenReturn("ghost@nemblex.dev");
        when(userRepository.findByEmail("ghost@nemblex.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveId_shouldReturnTheUsersId() {
        AppUser user = AppUser.builder().id(9L).email("carlos.mendez@nemblex.dev").build();
        when(authentication.getName()).thenReturn("carlos.mendez@nemblex.dev");
        when(userRepository.findByEmail("carlos.mendez@nemblex.dev")).thenReturn(Optional.of(user));

        assertThat(resolver.resolveId(authentication)).isEqualTo(9L);
    }
}
