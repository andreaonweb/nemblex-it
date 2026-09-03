package com.nemblex.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String action;

    private String reason;
}
