package com.nemblex.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class DataSeederTest {

    @Test
    void shouldOnlyBeActiveOnDevProfile() {
        // Arrange
        Profile profile = DataSeeder.class.getAnnotation(Profile.class);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev");
    }
}
