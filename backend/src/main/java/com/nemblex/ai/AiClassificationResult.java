package com.nemblex.ai;

import com.nemblex.entity.enums.TicketPriority;

public record AiClassificationResult(String category, TicketPriority priority, String reasoning,
                                      String employeeMessage, ActionProposal actionProposal) {

    public AiClassificationResult(String category, TicketPriority priority, String reasoning, String employeeMessage) {
        this(category, priority, reasoning, employeeMessage, null);
    }
}
