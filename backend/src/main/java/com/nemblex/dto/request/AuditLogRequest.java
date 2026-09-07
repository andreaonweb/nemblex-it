package com.nemblex.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLogRequest {

    @NotNull(message = "ticketId is required")
    private Long ticketId;

    @NotBlank(message = "action is required")
    @Size(max = 50, message = "action must be at most 50 characters")
    private String action;

    private String reason;
}
