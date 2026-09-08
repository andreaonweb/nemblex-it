package com.nemblex.service.impl;

import com.nemblex.ai.AiClassificationResult;
import com.nemblex.ai.GeminiClient;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.Ticket;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.TicketRepository;
import com.nemblex.service.interfaces.AuditLogService;
import com.nemblex.service.interfaces.TicketAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketAiServiceImpl implements TicketAiService {

    private static final Logger log = LoggerFactory.getLogger(TicketAiServiceImpl.class);

    private final TicketRepository ticketRepository;
    private final GeminiClient geminiClient;
    private final AuditLogService auditLogService;

    public TicketAiServiceImpl(TicketRepository ticketRepository,
                               GeminiClient geminiClient,
                               AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.geminiClient = geminiClient;
        this.auditLogService = auditLogService;
    }

    @Override
    public AuditLogResponse classifyTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        AiClassificationResult classification = geminiClient
                .classifyTicket(ticket.getTitle(), ticket.getDescription())
                .orElseThrow(() -> new BadRequestException(
                        "No se pudo obtener una clasificacion de IA para el ticket " + ticketId));

        log.info("AI classification for ticket {}: category={}, priority={}, reasoning={}",
                ticketId, classification.category(), classification.priority(), classification.reasoning());

        return auditLogService.createAiClassification(ticketId, classification.reasoning());
    }
}
