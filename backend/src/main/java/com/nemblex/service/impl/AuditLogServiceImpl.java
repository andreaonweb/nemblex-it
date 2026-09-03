package com.nemblex.service.impl;

import com.nemblex.dto.request.AuditLogApprovalRequest;
import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.AuditLog;
import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.AuditResultStatus;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.mapper.AuditLogMapper;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.repository.AuditLogRepository;
import com.nemblex.repository.TicketRepository;
import com.nemblex.service.interfaces.AuditLogService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               TicketRepository ticketRepository,
                               AppUserRepository userRepository,
                               AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public AuditLogResponse createLog(AuditLogRequest dto) {
        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", dto.getTicketId()));

        AuditLog auditLog = auditLogMapper.toEntity(dto);
        auditLog.setTicket(ticket);
        auditLog.setResultStatus(AuditResultStatus.PENDING);

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByTicket(Long ticketId) {
        return auditLogRepository.findByTicketId(ticketId).stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllPending() {
        return auditLogRepository.findByResultStatus(AuditResultStatus.PENDING).stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Override
    public AuditLogResponse resolveLog(Long id, AuditLogApprovalRequest dto, Long approvedByUserId) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));

        if (auditLog.getResultStatus() != AuditResultStatus.PENDING) {
            throw new BadRequestException(
                    "AuditLog " + id + " is already resolved with status " + auditLog.getResultStatus());
        }

        AuditResultStatus target = dto.getResultStatus();
        if (target != AuditResultStatus.APPROVED && target != AuditResultStatus.REJECTED) {
            throw new BadRequestException("resultStatus must be APPROVED or REJECTED");
        }

        AppUser approver = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", approvedByUserId));

        auditLog.setResultStatus(target);
        auditLog.setApprovedBy(approver);

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }
}
