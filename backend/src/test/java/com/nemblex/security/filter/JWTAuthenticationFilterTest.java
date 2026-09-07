package com.nemblex.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemblex.security.CustomAuthenticationManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JWTAuthenticationFilterTest {

    @Test
    void unsuccessfulAuthentication_shouldWriteStandardErrorResponseShape() throws Exception {
        // Arrange
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(
                Mockito.mock(CustomAuthenticationManager.class), "test-secret", 3600000L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.unsuccessfulAuthentication(request, response, new BadCredentialsException("some internal detail"));

        // Assert
        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asText()).isEqualTo("Invalid email or password");
        assertThat(body.get("path").asText()).isEqualTo("/api/auth/login");
        assertThat(body.has("timestamp")).isTrue();
    }
}
