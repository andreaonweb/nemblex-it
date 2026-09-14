package com.nemblex.service.impl;

import com.nemblex.dto.request.AuditLogApprovalRequest;
import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.dto.response.PagedResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.AuditLog;
import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.AuditResultStatus;
import com.nemblex.entity.enums.Role;
import com.nemblex.entity.enums.TicketAction;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ForbiddenException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.mapper.AuditLogMapper;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.repository.AuditLogRepository;
import com.nemblex.repository.CategoryRepository;
import com.nemblex.repository.TicketRepository;
import com.nemblex.service.interfaces.AuditLogService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               TicketRepository ticketRepository,
                               AppUserRepository userRepository,
                               CategoryRepository categoryRepository,
                               AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public AuditLogResponse createLog(AuditLogRequest dto) {
        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", dto.getTicketId()));
        if (ticket.getStatus() != TicketStatus.PENDING_APPROVAL) {
            throw new BadRequestException(
                    "Solo se pueden registrar auditorias sobre incidencias en estado PENDING_APPROVAL");
        }

        AuditLog auditLog = auditLogMapper.toEntity(dto);
        auditLog.setTicket(ticket);
        auditLog.setResultStatus(AuditResultStatus.PENDING);

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByTicket(Long ticketId, AppUser requestingUser) {
        if (requestingUser.getRole() == Role.EMPLOYEE) {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
            if (!ticket.getCreatedBy().getId().equals(requestingUser.getId())) {
                throw new ForbiddenException("No tenes acceso a la auditoria de este ticket");
            }
        }

        return auditLogRepository.findByTicketId(ticketId).stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAllPending(Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByResultStatus(AuditResultStatus.PENDING, pageable);
        return PagedResponse.from(page.map(auditLogMapper::toResponse));
    }

    @Override
    public AuditLogResponse resolveLog(Long id, AuditLogApprovalRequest dto, Long approvedByUserId) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría", "id", id));

        if (auditLog.getResultStatus() != AuditResultStatus.PENDING) {
            throw new BadRequestException(
                    "El registro de auditoría " + id + " ya está resuelto con estado " + auditLog.getResultStatus());
        }

        AuditResultStatus target = dto.getResultStatus();
        if (target != AuditResultStatus.APPROVED && target != AuditResultStatus.REJECTED) {
            throw new BadRequestException("resultStatus debe ser APPROVED o REJECTED");
        }

        AppUser approver = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", approvedByUserId));

        auditLog.setResultStatus(target);
        auditLog.setApprovedBy(approver);

        if (target == AuditResultStatus.APPROVED && applyApprovedAction(auditLog)) {
            ticketRepository.saveAndFlush(auditLog.getTicket());
        }

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }

    private boolean applyApprovedAction(AuditLog auditLog) {
        Optional<TicketAction> action = TicketAction.fromString(auditLog.getAction());
        if (action.isEmpty()) {
            return false;
        }

        Ticket ticket = auditLog.getTicket();
        switch (action.get()) {
            case CLOSE -> ticket.setStatus(TicketStatus.RESOLVED);
            case ESCALATE -> ticket.setStatus(TicketStatus.IN_PROGRESS);
            case REASSIGN -> ticket.setAssignedTo(null);
            case AI_CLASSIFY -> applyAiClassification(auditLog, ticket);
        }
        return true;
    }

    private void applyAiClassification(AuditLog auditLog, Ticket ticket) {
        if (auditLog.getProposedPriority() != null) {
            ticket.setPriority(auditLog.getProposedPriority());
        }
        if (auditLog.getProposedCategory() != null) {
            categoryRepository.findByNameIgnoreCase(auditLog.getProposedCategory())
                    .ifPresent(ticket::setCategory);
        }
    }

    @Override
    public AuditLogResponse resolveDirectly(AuditLogRequest dto, Long technicianId) {
        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", dto.getTicketId()));
        if (ticket.getStatus().isTerminal()) {
            throw new BadRequestException(
                    "El ticket " + ticket.getId() + " ya está " + ticket.getStatus() + ", no hay nada que resolver");
        }

        AppUser technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", technicianId));

        AuditLog auditLog = auditLogMapper.toEntity(dto);
        auditLog.setTicket(ticket);
        auditLog.setResultStatus(AuditResultStatus.APPROVED);
        auditLog.setApprovedBy(technician);

        ticket.setStatus(TicketStatus.RESOLVED);
        ticketRepository.saveAndFlush(ticket);

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }

    @Override
    public AuditLogResponse undoResolution(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría", "id", id));

        if (auditLog.getResultStatus() == AuditResultStatus.PENDING) {
            throw new BadRequestException("El registro de auditoría no está resuelto, no hay nada que deshacer");
        }

        auditLog.setResultStatus(AuditResultStatus.PENDING);
        auditLog.setApprovedBy(null);

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }

    @Override
    public AuditLogResponse createAiProposal(Long ticketId, TicketAction action, String reasoning,
                                              String employeeMessage) {
        Ticket ticket = findActionableTicket(ticketId);

        AuditLog auditLog = AuditLog.builder()
                .ticket(ticket)
                .action(action.name())
                .reason(reasoning)
                .employeeMessage(employeeMessage)
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }

    @Override
    public AuditLogResponse createAiClassificationProposal(Long ticketId, String reasoning, String employeeMessage,
                                                             String category, TicketPriority priority) {
        Ticket ticket = findActionableTicket(ticketId);

        AuditLog auditLog = AuditLog.builder()
                .ticket(ticket)
                .action(TicketAction.AI_CLASSIFY.name())
                .reason(reasoning)
                .employeeMessage(employeeMessage)
                .proposedCategory(category)
                .proposedPriority(priority)
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        return auditLogMapper.toResponse(auditLogRepository.saveAndFlush(auditLog));
    }

    private Ticket findActionableTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        if (ticket.getStatus().isTerminal()) {
            throw new BadRequestException(
                    "El ticket " + ticket.getId() + " ya está " + ticket.getStatus() + ", no hay nada que proponer");
        }
        return ticket;
    }
}
