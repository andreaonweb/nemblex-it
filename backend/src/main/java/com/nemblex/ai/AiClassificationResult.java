package com.nemblex.ai;

import com.nemblex.entity.enums.TicketPriority;

public record AiClassificationResult(String category, TicketPriority priority, String reasoning) {
}
