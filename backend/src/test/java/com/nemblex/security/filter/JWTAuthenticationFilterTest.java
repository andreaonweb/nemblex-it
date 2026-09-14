package com.nemblex.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.enums.Role;
import com.nemblex.security.AppUserDetails;
import com.nemblex.security.CustomAuthenticationManager;
import com.nemblex.security.SecurityConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
        assertThat(body.get("error").asText()).isEqualTo("No autorizado");
        assertThat(body.get("message").asText()).isEqualTo("Correo electrónico o contraseña incorrectos");
        assertThat(body.get("path").asText()).isEqualTo("/api/auth/login");
        assertThat(body.has("timestamp")).isTrue();
    }

    @Test
    void successfulAuthentication_shouldEmbedTheUserIdClaim_whenPrincipalIsAppUserDetails() throws Exception {
        // Arrange
        String secret = "test-secret";
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(
                Mockito.mock(CustomAuthenticationManager.class), secret, 3600000L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser appUser = AppUser.builder().id(42L).email("ana@nemblex.dev").role(Role.TECHNICIAN).build();
        Authentication authResult = new UsernamePasswordAuthenticationToken(
                new AppUserDetails(appUser), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")));

        // Act
        filter.successfulAuthentication(request, response, null, authResult);

        // Assert
        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        DecodedJWT decoded = JWT.require(Algorithm.HMAC512(secret)).build().verify(body.get("token").asText());
        assertThat(decoded.getClaim(SecurityConstants.USER_ID_CLAIM).asLong()).isEqualTo(42L);
    }
}
