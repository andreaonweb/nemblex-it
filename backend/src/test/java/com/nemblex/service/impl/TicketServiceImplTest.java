package com.nemblex.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nemblex.dto.request.TicketRequest;
import com.nemblex.dto.request.TicketUpdateRequest;
import com.nemblex.dto.response.PagedResponse;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.dto.response.TicketStatsResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.Role;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;
import com.nemblex.event.TicketCreatedEvent;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.mapper.TicketMapper;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.repository.CategoryRepository;
import com.nemblex.repository.TicketRepository;
import com.nemblex.repository.TicketStatusPriorityCount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void createTicket_shouldCreateWithDefaultStatusNew() {
        // Arrange
        TicketRequest request = TicketRequest.builder()
                .title("Impresora no enciende")
                .description("No prende desde ayer")
                .build();
        AppUser creator = AppUser.builder().id(1L).name("Ana").build();
        Ticket mappedEntity = new Ticket();
        mappedEntity.setTitle(request.getTitle());
        mappedEntity.setDescription(request.getDescription());
        TicketResponse expectedResponse = TicketResponse.builder()
                .id(10L)
                .title(request.getTitle())
                .status(TicketStatus.NEW)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(ticketMapper.toEntity(request)).thenReturn(mappedEntity);
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMapper.toResponse(mappedEntity)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.createTicket(request, 1L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).saveAndFlush(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(savedTicket.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(savedTicket.getCreatedBy()).isEqualTo(creator);
    }

    @Test
    void createTicket_shouldPublishTicketCreatedEvent_afterSavingTheTicket() {
        // Arrange
        TicketRequest request = TicketRequest.builder()
                .title("Impresora no enciende")
                .description("No prende desde ayer")
                .build();
        AppUser creator = AppUser.builder().id(1L).name("Ana").build();
        Ticket mappedEntity = new Ticket();
        mappedEntity.setTitle(request.getTitle());
        mappedEntity.setDescription(request.getDescription());
        Ticket savedTicket = Ticket.builder().id(42L).title(request.getTitle()).build();
        TicketResponse expectedResponse = TicketResponse.builder().id(42L).title(request.getTitle()).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(ticketMapper.toEntity(request)).thenReturn(mappedEntity);
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenReturn(savedTicket);
        when(ticketMapper.toResponse(savedTicket)).thenReturn(expectedResponse);

        // Act
        ticketService.createTicket(request, 1L);

        // Assert
        ArgumentCaptor<TicketCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TicketCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().ticketId()).isEqualTo(42L);
    }

    @Test
    void getTicketById_shouldReturnTicket_whenExists() {
        // Arrange
        Ticket ticket = Ticket.builder().id(5L).title("Sin red WiFi").build();
        TicketResponse expectedResponse = TicketResponse.builder().id(5L).title("Sin red WiFi").build();
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.getTicketById(5L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getTicketById_shouldThrowResourceNotFoundException_whenNotExists() {
        // Arrange
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    void getAllTickets_shouldSearchWithFiltersAndPagination() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.NEW).build();
        TicketResponse response = TicketResponse.builder().id(1L).status(TicketStatus.NEW).build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);
        when(ticketRepository.search(TicketStatus.NEW, TicketPriority.HIGH, 3L, null, "%vpn%", pageable))
                .thenReturn(page);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        // Act
        PagedResponse<TicketResponse> result =
                ticketService.getAllTickets(TicketStatus.NEW, TicketPriority.HIGH, 3L, "vpn", pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(ticketRepository).search(TicketStatus.NEW, TicketPriority.HIGH, 3L, null, "%vpn%", pageable);
    }

    @Test
    void getAllTickets_shouldNormalizeBlankSearchToNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Ticket> page = new PageImpl<>(List.of(), pageable, 0);
        when(ticketRepository.search(null, null, null, null, null, pageable)).thenReturn(page);

        // Act
        ticketService.getAllTickets(null, null, null, "   ", pageable);

        // Assert
        verify(ticketRepository).search(null, null, null, null, null, pageable);
    }

    @Test
    void getStats_shouldSumOpenAndCriticalCountsAcrossAllTickets() {
        // Arrange
        List<TicketStatusPriorityCount> counts = List.of(
                statusPriorityCount(TicketStatus.NEW, TicketPriority.HIGH, 2),
                statusPriorityCount(TicketStatus.IN_PROGRESS, TicketPriority.LOW, 3),
                statusPriorityCount(TicketStatus.RESOLVED, TicketPriority.HIGH, 5));
        when(ticketRepository.countByStatusAndPriority(null)).thenReturn(counts);

        // Act
        TicketStatsResponse result = ticketService.getStats();

        // Assert
        assertThat(result.getAbiertas()).isEqualTo(5);
        assertThat(result.getCriticas()).isEqualTo(2);
        assertThat(result.getByStatus()).containsEntry(TicketStatus.NEW, 2L);
        assertThat(result.getByStatus()).containsEntry(TicketStatus.IN_PROGRESS, 3L);
        assertThat(result.getByStatus()).containsEntry(TicketStatus.RESOLVED, 5L);
    }

    @Test
    void getMyStats_shouldScopeCountsToTheGivenUser() {
        // Arrange
        List<TicketStatusPriorityCount> counts =
                List.of(statusPriorityCount(TicketStatus.NEW, TicketPriority.MEDIUM, 1));
        when(ticketRepository.countByStatusAndPriority(7L)).thenReturn(counts);

        // Act
        TicketStatsResponse result = ticketService.getMyStats(7L);

        // Assert
        assertThat(result.getAbiertas()).isEqualTo(1);
        assertThat(result.getCriticas()).isZero();
        verify(ticketRepository).countByStatusAndPriority(7L);
    }

    private static TicketStatusPriorityCount statusPriorityCount(TicketStatus status, TicketPriority priority,
                                                                   long count) {
        TicketStatusPriorityCount mock = org.mockito.Mockito.mock(TicketStatusPriorityCount.class);
        org.mockito.Mockito.lenient().when(mock.getStatus()).thenReturn(status);
        org.mockito.Mockito.lenient().when(mock.getPriority()).thenReturn(priority);
        org.mockito.Mockito.lenient().when(mock.getCount()).thenReturn(count);
        return mock;
    }

    @Test
    void updateTicket_shouldUpdateFields_whenExists() {
        // Arrange
        Ticket existingTicket = Ticket.builder()
                .id(1L)
                .title("Titulo viejo")
                .status(TicketStatus.NEW)
                .priority(TicketPriority.MEDIUM)
                .build();
        TicketUpdateRequest request = TicketUpdateRequest.builder().title("Titulo nuevo").build();
        TicketResponse expectedResponse = TicketResponse.builder().id(1L).title("Titulo nuevo").build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(ticketRepository.saveAndFlush(existingTicket)).thenReturn(existingTicket);
        when(ticketMapper.toResponse(existingTicket)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.updateTicket(1L, request);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(ticketMapper).updateFromRequest(request, existingTicket);
        verify(categoryRepository, never()).findById(anyLong());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void updateTicket_shouldThrowResourceNotFoundException_whenNotExists() {
        // Arrange
        TicketUpdateRequest request = TicketUpdateRequest.builder().title("Titulo nuevo").build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.updateTicket(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketMapper, never()).updateFromRequest(any(), any());
    }

    @Test
    void updateTicket_shouldThrowBadRequestException_whenSettingStatusResolvedDirectly() {
        // Arrange
        Ticket existingTicket = Ticket.builder()
                .id(1L)
                .status(TicketStatus.PENDING_APPROVAL)
                .build();
        TicketUpdateRequest request = TicketUpdateRequest.builder().status(TicketStatus.RESOLVED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.updateTicket(1L, request))
                .isInstanceOf(BadRequestException.class);
        verify(ticketMapper, never()).updateFromRequest(any(), any());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTicket_shouldAllowSettingStatusClosed_directly() {
        // Arrange
        Ticket existingTicket = Ticket.builder().id(1L).status(TicketStatus.RESOLVED).build();
        TicketUpdateRequest request = TicketUpdateRequest.builder().status(TicketStatus.CLOSED).build();
        TicketResponse expectedResponse = TicketResponse.builder().id(1L).status(TicketStatus.CLOSED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(ticketRepository.saveAndFlush(existingTicket)).thenReturn(existingTicket);
        when(ticketMapper.toResponse(existingTicket)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.updateTicket(1L, request);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(ticketMapper).updateFromRequest(request, existingTicket);
    }

    @Test
    void deleteTicket_shouldCallRepositoryDelete_whenExists() {
        // Arrange
        Ticket existingTicket = Ticket.builder().id(1L).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        // Act
        ticketService.deleteTicket(1L);

        // Assert
        verify(ticketRepository).delete(existingTicket);
    }

    @Test
    void deleteTicket_shouldThrowResourceNotFoundException_whenNotExists() {
        // Arrange
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.deleteTicket(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).delete(any());
    }

    @Test
    void assignToMe_shouldAssignTicket_whenActive() {
        // Arrange
        Ticket existingTicket = Ticket.builder().id(1L).status(TicketStatus.NEW).build();
        AppUser technician = AppUser.builder().id(3L).name("Ana Torres").build();
        TicketResponse expectedResponse = TicketResponse.builder().id(1L).assignedToId(3L).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findById(3L)).thenReturn(Optional.of(technician));
        when(ticketRepository.saveAndFlush(existingTicket)).thenReturn(existingTicket);
        when(ticketMapper.toResponse(existingTicket)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.assignToMe(1L, 3L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(existingTicket.getAssignedTo()).isEqualTo(technician);
    }

    @Test
    void assignToMe_shouldThrowResourceNotFoundException_whenTicketNotExists() {
        // Arrange
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignToMe(1L, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignToMe_shouldThrowBadRequestException_whenTicketResolved() {
        // Arrange
        Ticket existingTicket = Ticket.builder().id(1L).status(TicketStatus.RESOLVED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignToMe(1L, 3L))
                .isInstanceOf(BadRequestException.class);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignToMe_shouldThrowBadRequestException_whenTicketClosed() {
        // Arrange
        Ticket existingTicket = Ticket.builder().id(1L).status(TicketStatus.CLOSED).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignToMe(1L, 3L))
                .isInstanceOf(BadRequestException.class);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignToMe_shouldThrowBadRequestException_whenAlreadyAssignedToAnotherUser() {
        // Arrange
        AppUser otherTechnician = AppUser.builder().id(9L).name("Beatriz Ruiz").build();
        Ticket existingTicket = Ticket.builder()
                .id(1L)
                .status(TicketStatus.NEW)
                .assignedTo(otherTechnician)
                .build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignToMe(1L, 3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya esta asignado a otro tecnico");
        verify(ticketRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void assignToMe_shouldBeIdempotent_whenAlreadyAssignedToTheSameUser() {
        // Arrange
        AppUser technician = AppUser.builder().id(3L).name("Ana Torres").build();
        Ticket existingTicket = Ticket.builder()
                .id(1L)
                .status(TicketStatus.NEW)
                .assignedTo(technician)
                .build();
        TicketResponse expectedResponse = TicketResponse.builder().id(1L).assignedToId(3L).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(ticketMapper.toResponse(existingTicket)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.assignToMe(1L, 3L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void unassign_shouldClearAssignment_whenRequestedByTheAssignee() {
        // Arrange
        AppUser technician = AppUser.builder().id(3L).name("Ana Torres").role(Role.TECHNICIAN).build();
        Ticket existingTicket = Ticket.builder().id(1L).assignedTo(technician).build();
        TicketResponse expectedResponse = TicketResponse.builder().id(1L).assignedToId(null).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findById(3L)).thenReturn(Optional.of(technician));
        when(ticketRepository.saveAndFlush(existingTicket)).thenReturn(existingTicket);
        when(ticketMapper.toResponse(existingTicket)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.unassign(1L, 3L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(existingTicket.getAssignedTo()).isNull();
    }

    @Test
    void unassign_shouldClearAssignment_whenRequestedBySupervisor_evenIfNotTheAssignee() {
        // Arrange
        AppUser technician = AppUser.builder().id(3L).name("Ana Torres").role(Role.TECHNICIAN).build();
        AppUser supervisor = AppUser.builder().id(2L).name("Beatriz Ruiz").role(Role.SUPERVISOR).build();
        Ticket existingTicket = Ticket.builder().id(1L).assignedTo(technician).build();
        TicketResponse expectedResponse = TicketResponse.builder().id(1L).assignedToId(null).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(supervisor));
        when(ticketRepository.saveAndFlush(existingTicket)).thenReturn(existingTicket);
        when(ticketMapper.toResponse(existingTicket)).thenReturn(expectedResponse);

        // Act
        TicketResponse result = ticketService.unassign(1L, 2L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(existingTicket.getAssignedTo()).isNull();
    }

    @Test
    void unassign_shouldThrowAccessDeniedException_whenRequestedByAnotherTechnician() {
        // Arrange
        AppUser assignee = AppUser.builder().id(3L).name("Ana Torres").role(Role.TECHNICIAN).build();
        AppUser otherTechnician = AppUser.builder().id(4L).name("Carlos Mendez").role(Role.TECHNICIAN).build();
        Ticket existingTicket = Ticket.builder().id(1L).assignedTo(assignee).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(userRepository.findById(4L)).thenReturn(Optional.of(otherTechnician));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.unassign(1L, 4L))
                .isInstanceOf(AccessDeniedException.class);
        verify(ticketRepository, never()).saveAndFlush(any());
        assertThat(existingTicket.getAssignedTo()).isEqualTo(assignee);
    }

    @Test
    void unassign_shouldThrowBadRequestException_whenTicketHasNoAssignee() {
        // Arrange
        Ticket existingTicket = Ticket.builder().id(1L).assignedTo(null).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.unassign(1L, 3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no esta asignado a nadie");
        verify(userRepository, never()).findById(anyLong());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMyTickets_shouldReturnOnlyTicketsCreatedByTheGivenUser() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).build();
        TicketResponse response = TicketResponse.builder().id(1L).build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Ticket> page = new PageImpl<>(List.of(ticket), pageable, 1);
        when(ticketRepository.search(null, null, null, 7L, null, pageable)).thenReturn(page);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        // Act
        PagedResponse<TicketResponse> result = ticketService.getMyTickets(7L, null, null, null, pageable);

        // Assert
        assertThat(result.getContent()).containsExactly(response);
        verify(ticketRepository).search(null, null, null, 7L, null, pageable);
    }
}
