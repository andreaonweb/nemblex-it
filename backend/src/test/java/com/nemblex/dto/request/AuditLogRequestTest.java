package com.nemblex.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuditLogRequestTest {

    private final Validator validator;

    AuditLogRequestTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Test
    void shouldRejectAction_whenLongerThanFiftyCharacters() {
        // Arrange
        AuditLogRequest request = AuditLogRequest.builder()
                .ticketId(1L)
                .action("A".repeat(51))
                .build();

        // Act
        Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("action"));
    }

    @Test
    void shouldAcceptAction_whenFiftyCharactersOrFewer() {
        // Arrange
        AuditLogRequest request = AuditLogRequest.builder()
                .ticketId(1L)
                .action("A".repeat(50))
                .build();

        // Act
        Set<ConstraintViolation<AuditLogRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("action"));
    }
}
