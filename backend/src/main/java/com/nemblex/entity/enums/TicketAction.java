package com.nemblex.entity.enums;

import java.util.Optional;

public enum TicketAction {
    CLOSE,
    ESCALATE,
    REASSIGN,
    AI_CLASSIFY;

    public static Optional<TicketAction> fromString(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
