package com.nemblex.dto.response;

import com.nemblex.entity.enums.TicketStatus;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketStatsResponse {

    private long abiertas;
    private long criticas;
    private Map<TicketStatus, Long> byStatus;
}
