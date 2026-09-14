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

    @NotNull(message = "ticketId es obligatorio")
    private Long ticketId;

    @NotBlank(message = "action es obligatoria")
    @Size(max = 50, message = "action debe tener como máximo 50 caracteres")
    private String action;

    private String reason;
}
