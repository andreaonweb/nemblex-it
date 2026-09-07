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
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(body.getMessage()).doesNotContain("character varying");
    }
}
