package com.nemblex.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nemblex.entity.AppUser;
import com.nemblex.entity.enums.Role;
import com.nemblex.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DataSeederTest {

    @Test
    void shouldOnlyBeActiveOnDevProfile() {
        // Arrange
        Profile profile = DataSeeder.class.getAnnotation(Profile.class);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev");
    }

    @Test
    void run_shouldCreateSixEmployeeAccountsAmongTheSeedUsers_whenNoneExistYet() {
        // Arrange
        AppUserRepository userRepository = mock(AppUserRepository.class);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        DataSeeder seeder = new DataSeeder(userRepository, new BCryptPasswordEncoder());

        // Act
        seeder.run();

        // Assert
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository, org.mockito.Mockito.atLeast(6)).save(captor.capture());
        List<AppUser> saved = captor.getAllValues();
        List<String> employeeEmails = saved.stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .map(AppUser::getEmail)
                .toList();

        assertThat(employeeEmails).containsExactlyInAnyOrder(
                "carlos.mendez@nemblex.dev",
                "lucia.camara@nemblex.dev",
                "javier.ferrer@nemblex.dev",
                "paula.nogales@nemblex.dev",
                "sofia.duarte@nemblex.dev",
                "ricardo.olmos@nemblex.dev");
    }
}
