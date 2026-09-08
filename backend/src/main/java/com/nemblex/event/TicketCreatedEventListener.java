package com.nemblex.event;

import com.nemblex.service.interfaces.TicketAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TicketCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketCreatedEventListener.class);

    private final TicketAiService ticketAiService;

    public TicketCreatedEventListener(TicketAiService ticketAiService) {
        this.ticketAiService = ticketAiService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        try {
            ticketAiService.classifyTicket(event.ticketId());
        } catch (Exception ex) {
            log.error("Fallo la clasificacion automatica por IA para el ticket {}", event.ticketId(), ex);
        }
    }
}
