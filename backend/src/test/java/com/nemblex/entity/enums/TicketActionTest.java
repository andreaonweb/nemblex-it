package com.nemblex.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TicketActionTest {

    @Test
    void fromString_shouldReturnMatchingConstant_whenValueIsAKnownAction() {
        assertThat(TicketAction.fromString("CLOSE")).contains(TicketAction.CLOSE);
        assertThat(TicketAction.fromString("ESCALATE")).contains(TicketAction.ESCALATE);
        assertThat(TicketAction.fromString("REASSIGN")).contains(TicketAction.REASSIGN);
        assertThat(TicketAction.fromString("AI_CLASSIFY")).contains(TicketAction.AI_CLASSIFY);
    }

    @Test
    void fromString_shouldReturnEmpty_whenValueIsNotAKnownAction() {
        assertThat(TicketAction.fromString("APPROVE_RESOLUTION")).isEmpty();
        assertThat(TicketAction.fromString("close")).isEmpty();
    }

    @Test
    void fromString_shouldReturnEmpty_whenValueIsNull() {
        assertThat(TicketAction.fromString(null)).isEqualTo(Optional.empty());
    }
}
