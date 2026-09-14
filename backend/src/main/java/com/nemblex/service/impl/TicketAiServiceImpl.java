package com.nemblex.service.impl;

import com.nemblex.ai.ActionProposal;
import com.nemblex.ai.AiClassificationResult;
import com.nemblex.ai.GeminiClient;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.KnowledgeDocument;
import com.nemblex.entity.Ticket;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.KnowledgeRepository;
import com.nemblex.repository.TicketRepository;
import com.nemblex.service.interfaces.AuditLogService;
import com.nemblex.service.interfaces.TicketAiService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketAiServiceImpl implements TicketAiService {

    private static final Logger log = LoggerFactory.getLogger(TicketAiServiceImpl.class);

    private static final int CONTEXT_DOCUMENT_LIMIT = 2;

    private final TicketRepository ticketRepository;
    private final GeminiClient geminiClient;
    private final KnowledgeRepository knowledgeRepository;
    private final AuditLogService auditLogService;

    public TicketAiServiceImpl(TicketRepository ticketRepository,
                               GeminiClient geminiClient,
                               KnowledgeRepository knowledgeRepository,
                               AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.geminiClient = geminiClient;
        this.knowledgeRepository = knowledgeRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public AuditLogResponse classifyTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        List<String> context = findRelevantContext(ticket);

        AiClassificationResult classification = geminiClient
                .classifyTicket(ticket.getTitle(), ticket.getDescription(), context)
                .orElseThrow(() -> new BadRequestException(
                        "No se pudo obtener una clasificacion de IA para el ticket " + ticketId));

        log.info("AI classification for ticket {}: category={}, priority={}, reasoning={}, contextDocs={}",
                ticketId, classification.category(), classification.priority(), classification.reasoning(),
                context.size());

        AuditLogResponse classificationLog = auditLogService.createAiClassificationProposal(
                ticketId, classification.reasoning(), classification.category(), classification.priority());

        ActionProposal proposal = classification.actionProposal();
        if (proposal != null) {
            log.info("AI action proposal for ticket {}: action={}, reason={}",
                    ticketId, proposal.action(), proposal.reason());
            auditLogService.createAiProposal(ticketId, proposal.action(), proposal.reason());
        }

        return classificationLog;
    }

    private List<String> findRelevantContext(Ticket ticket) {
        String ticketText = ticket.getTitle() + "\n" + ticket.getDescription();
        return geminiClient.embedText(ticketText)
                .map(embedding -> knowledgeRepository.findNearest(embedding, CONTEXT_DOCUMENT_LIMIT).stream()
                        .map(KnowledgeDocument::content)
                        .toList())
                .orElseGet(List::of);
    }
}
