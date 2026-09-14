package com.nemblex.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketStatusTest {

    @Test
    void isTerminal_shouldBeTrue_forResolvedAndClosed() {
        assertThat(TicketStatus.RESOLVED.isTerminal()).isTrue();
        assertThat(TicketStatus.CLOSED.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_shouldBeFalse_forActiveStatuses() {
        assertThat(TicketStatus.NEW.isTerminal()).isFalse();
        assertThat(TicketStatus.AI_CLASSIFIED.isTerminal()).isFalse();
        assertThat(TicketStatus.IN_PROGRESS.isTerminal()).isFalse();
        assertThat(TicketStatus.PENDING_APPROVAL.isTerminal()).isFalse();
    }
}
