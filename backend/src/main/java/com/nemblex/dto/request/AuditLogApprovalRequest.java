package com.nemblex.dto.request;

import com.nemblex.entity.enums.AuditResultStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLogApprovalRequest {

    @NotNull(message = "resultStatus es obligatorio")
    private AuditResultStatus resultStatus;
}
