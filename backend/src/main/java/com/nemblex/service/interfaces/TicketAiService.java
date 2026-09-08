package com.nemblex.service.interfaces;

import com.nemblex.dto.response.AuditLogResponse;

public interface TicketAiService {

    AuditLogResponse classifyTicket(Long ticketId);
}
