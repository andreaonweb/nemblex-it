package com.nemblex.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nemblex.dto.request.AuditLogApprovalRequest;
import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.dto.response.PagedResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.AuditLog;
import com.nemblex.entity.Category;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void createLog_shouldCreateWithDefaultStatusPending() {
        // Arrange
        AuditLogRequest dto = AuditLogRequest.builder().ticketId(1L).action("APPROVE_RESOLUTION").build();
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.PENDING_APPROVAL).build();
        AuditLog mappedEntity = new AuditLog();
        mappedEntity.setAction(dto.getAction());
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(10L)
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(auditLogMapper.toEntity(dto)).thenReturn(mappedEntity);
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogMapper.toResponse(mappedEntity)).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.createLog(dto);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAndFlush(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getResultStatus()).isEqualTo(AuditResultStatus.PENDING);
        assertThat(savedLog.getTicket()).isEqualTo(ticket);
    }

    @Test
    void createLog_shouldThrowResourceNotFoundException_whenTicketNotExists() {
        // Arrange
        AuditLogRequest dto = AuditLogRequest.builder().ticketId(99L).action("APPROVE_RESOLUTION").build();
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.createLog(dto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogMapper, never()).toEntity(any());
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void createLog_shouldThrowBadRequestException_whenTicketNotPendingApproval() {
        // Arrange
        AuditLogRequest dto = AuditLogRequest.builder().ticketId(1L).action("APPROVE_RESOLUTION").build();
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.IN_PROGRESS).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.createLog(dto))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogMapper, never()).toEntity(any());
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void getLogsByTicket_shouldReturnLogsForTicket_whenRequesterIsSupervisor() {
        // Arrange
        AuditLog log = AuditLog.builder().id(1L).build();
        AuditLogResponse response = AuditLogResponse.builder().id(1L).build();
        AppUser supervisor = AppUser.builder().id(2L).role(Role.SUPERVISOR).build();
        when(auditLogRepository.findByTicketId(1L)).thenReturn(List.of(log));
        when(auditLogMapper.toResponse(log)).thenReturn(response);

        // Act
        List<AuditLogResponse> result = auditLogService.getLogsByTicket(1L, supervisor);

        // Assert
        assertThat(result).containsExactly(response);
        verify(auditLogRepository).findByTicketId(1L);
        verify(ticketRepository, never()).findById(any());
    }

    @Test
    void getLogsByTicket_shouldReturnLogs_whenEmployeeOwnsTicket() {
        // Arrange
        AppUser employee = AppUser.builder().id(9L).role(Role.EMPLOYEE).build();
        Ticket ticket = Ticket.builder().id(1L).createdBy(employee).build();
        AuditLog log = AuditLog.builder().id(1L).ticket(ticket).build();
        AuditLogResponse response = AuditLogResponse.builder().id(1L).build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(auditLogRepository.findByTicketId(1L)).thenReturn(List.of(log));
        when(auditLogMapper.toResponse(log)).thenReturn(response);

        // Act
        List<AuditLogResponse> result = auditLogService.getLogsByTicket(1L, employee);

        // Assert
        assertThat(result).containsExactly(response);
    }

    @Test
    void getLogsByTicket_shouldThrowForbiddenException_whenEmployeeDoesNotOwnTicket() {
        // Arrange
        AppUser owner = AppUser.builder().id(9L).role(Role.EMPLOYEE).build();
        AppUser otherEmployee = AppUser.builder().id(10L).role(Role.EMPLOYEE).build();
        Ticket ticket = Ticket.builder().id(1L).createdBy(owner).build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.getLogsByTicket(1L, otherEmployee))
                .isInstanceOf(ForbiddenException.class);
        verify(auditLogRepository, never()).findByTicketId(any());
    }

    @Test
    void getLogsByTicket_shouldThrowResourceNotFoundException_whenEmployeeAndTicketNotExists() {
        // Arrange
        AppUser employee = AppUser.builder().id(9L).role(Role.EMPLOYEE).build();
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.getLogsByTicket(99L, employee))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogRepository, never()).findByTicketId(any());
    }

    @Test
    void getAllPending_shouldReturnOnlyPendingLogs() {
        // Arrange
        AuditLog log = AuditLog.builder().id(1L).resultStatus(AuditResultStatus.PENDING).build();
        AuditLogResponse response = AuditLogResponse.builder().id(1L).resultStatus(AuditResultStatus.PENDING).build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);
        when(auditLogRepository.findByResultStatus(AuditResultStatus.PENDING, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(log)).thenReturn(response);

        // Act
        PagedResponse<AuditLogResponse> result = auditLogService.getAllPending(pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(auditLogRepository).findByResultStatus(AuditResultStatus.PENDING, pageable);
    }

    @Test
    void resolveLog_shouldResolveTicket_whenApprovedActionIsClose() {
        // Arrange
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.PENDING_APPROVAL).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).action("CLOSE")
                .resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.APPROVED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(1L)
                .resultStatus(AuditResultStatus.APPROVED)
                .approvedByName("Jefe IT")
                .build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.resolveLog(1L, dto, 2L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(existingLog.getResultStatus()).isEqualTo(AuditResultStatus.APPROVED);
        assertThat(existingLog.getApprovedBy()).isEqualTo(approver);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void resolveLog_shouldSetTicketInProgress_whenApprovedActionIsEscalate() {
        // Arrange
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.PENDING_APPROVAL).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).action("ESCALATE")
                .resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.APPROVED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(AuditLogResponse.builder().id(1L).build());

        // Act
        auditLogService.resolveLog(1L, dto, 2L);

        // Assert
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void resolveLog_shouldClearAssignedTo_whenApprovedActionIsReassign() {
        // Arrange
        AppUser previousAssignee = AppUser.builder().id(9L).name("Tecnico Nivel 1").build();
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.IN_PROGRESS).assignedTo(previousAssignee).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).action("REASSIGN")
                .resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.APPROVED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(AuditLogResponse.builder().id(1L).build());

        // Act
        auditLogService.resolveLog(1L, dto, 2L);

        // Assert
        assertThat(ticket.getAssignedTo()).isNull();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void resolveLog_shouldUpdateCategoryAndPriority_whenApprovedActionIsAiClassifyAndCategoryMatches() {
        // Arrange
        Category networkCategory = Category.builder().id(3L).name("Redes").build();
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.AI_CLASSIFIED).priority(TicketPriority.MEDIUM).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).action("AI_CLASSIFY")
                .proposedCategory("Redes").proposedPriority(TicketPriority.HIGH)
                .resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.APPROVED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(categoryRepository.findByNameIgnoreCase("Redes")).thenReturn(Optional.of(networkCategory));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(AuditLogResponse.builder().id(1L).build());

        // Act
        auditLogService.resolveLog(1L, dto, 2L);

        // Assert
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.getCategory()).isEqualTo(networkCategory);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AI_CLASSIFIED);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void resolveLog_shouldUpdateOnlyPriority_whenApprovedActionIsAiClassifyAndCategoryHasNoMatch() {
        // Arrange
        Category originalCategory = Category.builder().id(1L).name("General").build();
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.AI_CLASSIFIED)
                .priority(TicketPriority.MEDIUM).category(originalCategory).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).action("AI_CLASSIFY")
                .proposedCategory("Categoria Inexistente").proposedPriority(TicketPriority.LOW)
                .resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.APPROVED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(categoryRepository.findByNameIgnoreCase("Categoria Inexistente")).thenReturn(Optional.empty());
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(AuditLogResponse.builder().id(1L).build());

        // Act
        auditLogService.resolveLog(1L, dto, 2L);

        // Assert
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.LOW);
        assertThat(ticket.getCategory()).isEqualTo(originalCategory);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void resolveLog_shouldNotTouchTicket_whenRejected() {
        // Arrange
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.PENDING_APPROVAL).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).action("CLOSE")
                .resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.REJECTED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(1L)
                .resultStatus(AuditResultStatus.REJECTED)
                .approvedByName("Jefe IT")
                .build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.resolveLog(1L, dto, 2L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(existingLog.getResultStatus()).isEqualTo(AuditResultStatus.REJECTED);
        assertThat(existingLog.getApprovedBy()).isEqualTo(approver);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PENDING_APPROVAL);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolveLog_shouldNotTouchTicket_whenApprovedActionIsUnknown() {
        // Arrange
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.PENDING_APPROVAL).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).action("APPROVE_RESOLUTION")
                .resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.APPROVED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver));
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(AuditLogResponse.builder().id(1L).build());

        // Act
        auditLogService.resolveLog(1L, dto, 2L);

        // Assert
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PENDING_APPROVAL);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolveLog_shouldThrowBadRequestException_whenAlreadyResolved() {
        // Arrange
        AuditLog existingLog = AuditLog.builder().id(1L).resultStatus(AuditResultStatus.APPROVED).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.REJECTED).build();
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.resolveLog(1L, dto, 2L))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).findById(anyLong());
        verify(auditLogRepository, never()).saveAndFlush(any());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolveLog_shouldThrowResourceNotFoundException_whenLogNotExists() {
        // Arrange
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.APPROVED).build();
        when(auditLogRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.resolveLog(1L, dto, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void resolveDirectly_shouldResolveTicketAndCreateApprovedLog_whenTicketActive() {
        // Arrange
        AuditLogRequest dto = AuditLogRequest.builder().ticketId(1L).action("REINICIO_SERVICIO").build();
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.IN_PROGRESS).build();
        AppUser technician = AppUser.builder().id(3L).name("Ana Torres").build();
        AuditLog mappedEntity = new AuditLog();
        mappedEntity.setAction(dto.getAction());
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(20L)
                .resultStatus(AuditResultStatus.APPROVED)
                .approvedByName("Ana Torres")
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(3L)).thenReturn(Optional.of(technician));
        when(auditLogMapper.toEntity(dto)).thenReturn(mappedEntity);
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogMapper.toResponse(mappedEntity)).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.resolveDirectly(dto, 3L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(mappedEntity.getResultStatus()).isEqualTo(AuditResultStatus.APPROVED);
        assertThat(mappedEntity.getApprovedBy()).isEqualTo(technician);
        assertThat(mappedEntity.getTicket()).isEqualTo(ticket);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void resolveDirectly_shouldThrowResourceNotFoundException_whenTicketNotExists() {
        // Arrange
        AuditLogRequest dto = AuditLogRequest.builder().ticketId(99L).action("REINICIO_SERVICIO").build();
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.resolveDirectly(dto, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolveDirectly_shouldThrowBadRequestException_whenTicketAlreadyResolved() {
        // Arrange
        AuditLogRequest dto = AuditLogRequest.builder().ticketId(1L).action("REINICIO_SERVICIO").build();
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.RESOLVED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.resolveDirectly(dto, 3L))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolveDirectly_shouldThrowBadRequestException_whenTicketAlreadyClosed() {
        // Arrange
        AuditLogRequest dto = AuditLogRequest.builder().ticketId(1L).action("REINICIO_SERVICIO").build();
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.CLOSED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.resolveDirectly(dto, 3L))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void undoResolution_shouldRevertToPending_whenResolved() {
        // Arrange
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();
        AuditLog existingLog = AuditLog.builder()
                .id(1L)
                .resultStatus(AuditResultStatus.APPROVED)
                .approvedBy(approver)
                .build();
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(1L)
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(auditLogRepository.saveAndFlush(existingLog)).thenReturn(existingLog);
        when(auditLogMapper.toResponse(existingLog)).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.undoResolution(1L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(existingLog.getResultStatus()).isEqualTo(AuditResultStatus.PENDING);
        assertThat(existingLog.getApprovedBy()).isNull();
        verify(auditLogRepository).saveAndFlush(existingLog);
    }

    @Test
    void undoResolution_shouldThrowBadRequestException_whenAlreadyPending() {
        // Arrange
        AuditLog existingLog = AuditLog.builder().id(1L).resultStatus(AuditResultStatus.PENDING).build();
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.undoResolution(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("El registro de auditoría no está resuelto, no hay nada que deshacer");
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void undoResolution_shouldThrowResourceNotFoundException_whenNotExists() {
        // Arrange
        when(auditLogRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.undoResolution(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void createAiProposal_shouldCreatePendingAiClassifyLog_whenTicketActive() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.NEW).build();
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(30L)
                .ticketId(1L)
                .action("AI_CLASSIFY")
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.createAiProposal(
                1L, TicketAction.AI_CLASSIFY, "Palabras clave de VPN detectadas",
                "Esperá 15 minutos y volvé a intentar conectarte a la VPN.");

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());
        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getTicket()).isEqualTo(ticket);
        assertThat(savedLog.getAction()).isEqualTo("AI_CLASSIFY");
        assertThat(savedLog.getReason()).isEqualTo("Palabras clave de VPN detectadas");
        assertThat(savedLog.getEmployeeMessage()).isEqualTo("Esperá 15 minutos y volvé a intentar conectarte a la VPN.");
        assertThat(savedLog.getResultStatus()).isEqualTo(AuditResultStatus.PENDING);
        assertThat(savedLog.getApprovedBy()).isNull();
    }

    @Test
    void createAiProposal_shouldCreatePendingLogWithArbitraryAction_forAgentActionProposals() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.NEW).build();
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(31L)
                .ticketId(1L)
                .action("CLOSE")
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.createAiProposal(
                1L, TicketAction.CLOSE, "Ticket duplicado del #8", "Ya identificamos este problema, no necesitás hacer nada más.");

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("CLOSE");
        assertThat(captor.getValue().getEmployeeMessage())
                .isEqualTo("Ya identificamos este problema, no necesitás hacer nada más.");
    }

    @Test
    void createAiProposal_shouldThrowResourceNotFoundException_whenTicketNotExists() {
        // Arrange
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.createAiProposal(99L, TicketAction.AI_CLASSIFY, "reasoning", "mensaje"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void createAiProposal_shouldThrowBadRequestException_whenTicketAlreadyResolved() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.RESOLVED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.createAiProposal(1L, TicketAction.AI_CLASSIFY, "reasoning", "mensaje"))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void createAiProposal_shouldThrowBadRequestException_whenTicketAlreadyClosed() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.CLOSED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.createAiProposal(1L, TicketAction.AI_CLASSIFY, "reasoning", "mensaje"))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void createAiClassificationProposal_shouldCreatePendingLogWithCategoryAndPriority_whenTicketActive() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.NEW).build();
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(60L)
                .ticketId(1L)
                .action("AI_CLASSIFY")
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = auditLogService.createAiClassificationProposal(
                1L, "Palabras clave de VPN detectadas", "Esperá 15 minutos y volvé a intentar conectarte a la VPN.",
                "Redes", TicketPriority.HIGH);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());
        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getTicket()).isEqualTo(ticket);
        assertThat(savedLog.getAction()).isEqualTo("AI_CLASSIFY");
        assertThat(savedLog.getReason()).isEqualTo("Palabras clave de VPN detectadas");
        assertThat(savedLog.getEmployeeMessage()).isEqualTo("Esperá 15 minutos y volvé a intentar conectarte a la VPN.");
        assertThat(savedLog.getProposedCategory()).isEqualTo("Redes");
        assertThat(savedLog.getProposedPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(savedLog.getResultStatus()).isEqualTo(AuditResultStatus.PENDING);
    }

    @Test
    void createAiClassificationProposal_shouldThrowResourceNotFoundException_whenTicketNotExists() {
        // Arrange
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.createAiClassificationProposal(
                99L, "reasoning", "mensaje", "Redes", TicketPriority.HIGH))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void createAiClassificationProposal_shouldThrowBadRequestException_whenTicketAlreadyResolved() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.RESOLVED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThatThrownBy(() -> auditLogService.createAiClassificationProposal(
                1L, "reasoning", "mensaje", "Redes", TicketPriority.HIGH))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogRepository, never()).saveAndFlush(any());
    }
}
