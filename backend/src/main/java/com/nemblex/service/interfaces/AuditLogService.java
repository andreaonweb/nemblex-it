package com.nemblex.service.interfaces;

import com.nemblex.dto.request.AuditLogApprovalRequest;
import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.dto.response.PagedResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.enums.TicketAction;
import com.nemblex.entity.enums.TicketPriority;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    AuditLogResponse createLog(AuditLogRequest dto);

    List<AuditLogResponse> getLogsByTicket(Long ticketId, AppUser requestingUser);

    PagedResponse<AuditLogResponse> getAllPending(Pageable pageable);

    AuditLogResponse resolveLog(Long id, AuditLogApprovalRequest dto, Long approvedByUserId);

    AuditLogResponse resolveDirectly(AuditLogRequest dto, Long technicianId);

    AuditLogResponse undoResolution(Long id);

    AuditLogResponse createAiProposal(Long ticketId, TicketAction action, String reasoning, String employeeMessage);

    AuditLogResponse createAiClassificationProposal(Long ticketId, String reasoning, String employeeMessage,
                                                      String category, TicketPriority priority);
}
