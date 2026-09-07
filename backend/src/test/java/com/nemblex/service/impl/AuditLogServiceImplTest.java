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
import com.nemblex.entity.AppUser;
import com.nemblex.entity.AuditLog;
import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.AuditResultStatus;
import com.nemblex.entity.enums.TicketStatus;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.mapper.AuditLogMapper;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.repository.AuditLogRepository;
import com.nemblex.repository.TicketRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AppUserRepository userRepository;

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
    void getLogsByTicket_shouldReturnLogsForTicket() {
        // Arrange
        AuditLog log = AuditLog.builder().id(1L).build();
        AuditLogResponse response = AuditLogResponse.builder().id(1L).build();
        when(auditLogRepository.findByTicketId(1L)).thenReturn(List.of(log));
        when(auditLogMapper.toResponse(log)).thenReturn(response);

        // Act
        List<AuditLogResponse> result = auditLogService.getLogsByTicket(1L);

        // Assert
        assertThat(result).containsExactly(response);
        verify(auditLogRepository).findByTicketId(1L);
    }

    @Test
    void getAllPending_shouldReturnOnlyPendingLogs() {
        // Arrange
        AuditLog log = AuditLog.builder().id(1L).resultStatus(AuditResultStatus.PENDING).build();
        AuditLogResponse response = AuditLogResponse.builder().id(1L).resultStatus(AuditResultStatus.PENDING).build();
        when(auditLogRepository.findByResultStatus(AuditResultStatus.PENDING)).thenReturn(List.of(log));
        when(auditLogMapper.toResponse(log)).thenReturn(response);

        // Act
        List<AuditLogResponse> result = auditLogService.getAllPending();

        // Assert
        assertThat(result).containsExactly(response);
        verify(auditLogRepository).findByResultStatus(AuditResultStatus.PENDING);
    }

    @Test
    void resolveLog_shouldApproveAndSaveApprover_whenPending() {
        // Arrange
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.PENDING_APPROVAL).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).resultStatus(AuditResultStatus.PENDING).build();
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
    void resolveLog_shouldRejectAndSaveApprover_whenPending() {
        // Arrange
        Ticket ticket = Ticket.builder().id(5L).status(TicketStatus.PENDING_APPROVAL).build();
        AuditLog existingLog = AuditLog.builder().id(1L).ticket(ticket).resultStatus(AuditResultStatus.PENDING).build();
        AuditLogApprovalRequest dto = AuditLogApprovalRequest.builder().resultStatus(AuditResultStatus.REJECTED).build();
        AppUser approver = AppUser.builder().id(2L).name("Jefe IT").build();
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(1L)
                .resultStatus(AuditResultStatus.REJECTED)
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
        assertThat(existingLog.getResultStatus()).isEqualTo(AuditResultStatus.REJECTED);
        assertThat(existingLog.getApprovedBy()).isEqualTo(approver);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketRepository).saveAndFlush(ticket);
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
                .hasMessage("AuditLog is not resolved, nothing to undo");
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
}
