package com.nemblex.dto.request;

import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketUpdateRequest {

    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long categoryId;
    private Long assignedTo;
}
