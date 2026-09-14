package com.nemblex.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGeneric_shouldNotLeakRawExceptionMessage() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/audit-logs");
        Exception ex = new RuntimeException(
                "ERROR: value too long for type character varying(50) - Detail: table audit_log");

        // Act
        ErrorResponse body = handler.handleGeneric(ex, request).getBody();

        // Assert
        assertThat(body.getMessage()).isEqualTo("Ocurrió un error inesperado");
        assertThat(body.getMessage()).doesNotContain("character varying");
    }

    @Test
    void handleForbidden_shouldReturn403WithMessage() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/audit-logs/ticket/1");
        ForbiddenException ex = new ForbiddenException("No tenes acceso a este ticket");

        // Act
        var response = handler.handleForbidden(ex, request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("No tenes acceso a este ticket");
        assertThat(response.getBody().getPath()).isEqualTo("/api/audit-logs/ticket/1");
    }
}
