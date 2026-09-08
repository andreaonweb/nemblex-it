package com.nemblex.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nemblex.exception.BadRequestException;
import com.nemblex.service.interfaces.TicketAiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketCreatedEventListenerTest {

    @Mock
    private TicketAiService ticketAiService;

    @Test
    void onTicketCreated_shouldInvokeClassifyTicket_withTheEventTicketId() {
        TicketCreatedEventListener listener = new TicketCreatedEventListener(ticketAiService);

        listener.onTicketCreated(new TicketCreatedEvent(7L));

        verify(ticketAiService).classifyTicket(7L);
    }

    @Test
    void onTicketCreated_shouldNotPropagateException_whenClassificationFails() {
        TicketCreatedEventListener listener = new TicketCreatedEventListener(ticketAiService);
        when(ticketAiService.classifyTicket(7L)).thenThrow(new BadRequestException("Gemini no responde"));

        assertThatCode(() -> listener.onTicketCreated(new TicketCreatedEvent(7L)))
                .doesNotThrowAnyException();
    }
}
