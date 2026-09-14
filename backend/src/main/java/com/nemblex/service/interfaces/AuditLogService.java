package com.nemblex.service.interfaces;

import com.nemblex.dto.request.AuditLogApprovalRequest;
import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.enums.TicketPriority;
import java.util.List;

public interface AuditLogService {

    AuditLogResponse createLog(AuditLogRequest dto);

    List<AuditLogResponse> getLogsByTicket(Long ticketId);

    List<AuditLogResponse> getAllPending();

    AuditLogResponse resolveLog(Long id, AuditLogApprovalRequest dto, Long approvedByUserId);

    AuditLogResponse resolveDirectly(AuditLogRequest dto, Long technicianId);

    AuditLogResponse undoResolution(Long id);

    AuditLogResponse createAiProposal(Long ticketId, String action, String reasoning);

    AuditLogResponse createAiClassificationProposal(Long ticketId, String reasoning, String category,
                                                      TicketPriority priority);
}
