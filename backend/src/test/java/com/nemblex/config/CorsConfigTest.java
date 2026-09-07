package com.nemblex.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_shouldTrimWhitespace_aroundCommaSeparatedOrigins() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins",
                "http://localhost:4200, https://app.nemblex.dev");

        // Act
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        var config = ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");

        // Assert
        assertThat(config.getAllowedOrigins())
                .containsExactly("http://localhost:4200", "https://app.nemblex.dev");
    }
}
