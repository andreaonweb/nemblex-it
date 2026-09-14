package com.nemblex.entity.enums;

public enum TicketStatus {
    NEW,
    AI_CLASSIFIED,
    IN_PROGRESS,
    PENDING_APPROVAL,
    RESOLVED,
    CLOSED;

    public boolean isTerminal() {
        return this == RESOLVED || this == CLOSED;
    }
}
