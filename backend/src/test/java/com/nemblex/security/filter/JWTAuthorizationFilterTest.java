package com.nemblex.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.nemblex.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JWTAuthorizationFilterTest {

    private final JWTAuthorizationFilter filter = new JWTAuthorizationFilter("test-secret");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldContinueChainWithoutAuthentication_whenTokenInvalid() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + "garbage-expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenTokenValid() throws Exception {
        // Arrange
        String token = JWT.create()
                .withSubject("ana@nemblex.dev")
                .withClaim(SecurityConstants.ROLE_CLAIM, "ROLE_TECHNICIAN")
                .withExpiresAt(new Date(System.currentTimeMillis() + 60000))
                .sign(Algorithm.HMAC512("test-secret"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        request.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("ana@nemblex.dev");
        verify(chain).doFilter(request, response);
    }
}
