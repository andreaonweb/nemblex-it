package com.nemblex.dto.response;

import com.nemblex.entity.enums.AuditResultStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private Long ticketId;
    private String action;
    private String reason;
    private AuditResultStatus resultStatus;
    private String approvedByName;
    private LocalDateTime createdAt;
}
