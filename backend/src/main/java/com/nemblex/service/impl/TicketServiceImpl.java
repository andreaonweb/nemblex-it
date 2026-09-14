package com.nemblex.service.impl;

import com.nemblex.dto.request.TicketRequest;
import com.nemblex.dto.request.TicketUpdateRequest;
import com.nemblex.dto.response.PagedResponse;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.dto.response.TicketStatsResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.Category;
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
import com.nemblex.service.interfaces.TicketService;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             AppUserRepository userRepository,
                             CategoryRepository categoryRepository,
                             TicketMapper ticketMapper,
                             ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ticketMapper = ticketMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TicketResponse createTicket(TicketRequest request, Long creatorId) {
        AppUser creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", creatorId));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setCreatedBy(creator);
        ticket.setCategory(resolveCategory(request.getCategoryId()));

        Ticket savedTicket = ticketRepository.saveAndFlush(ticket);
        eventPublisher.publishEvent(new TicketCreatedEvent(savedTicket.getId()));

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TicketResponse> getAllTickets(TicketStatus status, TicketPriority priority,
                                                        Long categoryId, String search, Pageable pageable) {
        Page<Ticket> page = ticketRepository.search(status, priority, categoryId, null,
                normalizeSearch(search), pageable);
        return PagedResponse.from(page.map(ticketMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatsResponse getStats() {
        return buildStats(null);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        return ticketMapper.toResponse(findTicketOrThrow(id));
    }

    @Override
    public TicketResponse updateTicket(Long id, TicketUpdateRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        if (request.getStatus() == TicketStatus.RESOLVED) {
            throw new BadRequestException(
                    "El estado RESOLVED solo puede alcanzarse aprobando un AuditLog");
        }
        ticketMapper.updateFromRequest(request, ticket);

        if (request.getCategoryId() != null) {
            ticket.setCategory(resolveCategory(request.getCategoryId()));
        }
        if (request.getAssignedTo() != null) {
            ticket.setAssignedTo(userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getAssignedTo())));
        }

        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public void deleteTicket(Long id) {
        ticketRepository.delete(findTicketOrThrow(id));
    }

    @Override
    public TicketResponse assignToMe(Long id, Long userId) {
        Ticket ticket = findTicketOrThrow(id);
        if (ticket.getStatus().isTerminal()) {
            throw new BadRequestException(
                    "El ticket " + id + " ya está " + ticket.getStatus() + ", no se puede asignar");
        }

        AppUser currentAssignee = ticket.getAssignedTo();
        if (currentAssignee != null) {
            if (currentAssignee.getId().equals(userId)) {
                return ticketMapper.toResponse(ticket);
            }
            throw new BadRequestException("Este ticket ya esta asignado a otro tecnico");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));
        ticket.setAssignedTo(user);

        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public TicketResponse unassign(Long id, Long userId) {
        Ticket ticket = findTicketOrThrow(id);
        AppUser assignee = ticket.getAssignedTo();
        if (assignee == null) {
            throw new BadRequestException("El ticket no esta asignado a nadie");
        }

        AppUser requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));

        boolean isAssignee = assignee.getId().equals(userId);
        boolean isPrivileged = requester.getRole() == Role.SUPERVISOR || requester.getRole() == Role.ADMIN;
        if (!isAssignee && !isPrivileged) {
            throw new AccessDeniedException("No tenes permiso para liberar esta asignacion");
        }

        ticket.setAssignedTo(null);
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TicketResponse> getMyTickets(Long userId, TicketStatus status, TicketPriority priority,
                                                       String search, Pageable pageable) {
        Page<Ticket> page = ticketRepository.search(status, priority, null, userId,
                normalizeSearch(search), pageable);
        return PagedResponse.from(page.map(ticketMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatsResponse getMyStats(Long userId) {
        return buildStats(userId);
    }

    private TicketStatsResponse buildStats(Long createdById) {
        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        long abiertas = 0;
        long criticas = 0;
        for (TicketStatusPriorityCount count : ticketRepository.countByStatusAndPriority(createdById)) {
            byStatus.merge(count.getStatus(), count.getCount(), Long::sum);
            if (!count.getStatus().isTerminal()) {
                abiertas += count.getCount();
                if (count.getPriority() == TicketPriority.HIGH) {
                    criticas += count.getCount();
                }
            }
        }
        return TicketStatsResponse.builder().abiertas(abiertas).criticas(criticas).byStatus(byStatus).build();
    }

    private String normalizeSearch(String search) {
        return (search == null || search.isBlank()) ? null : "%" + search.trim().toLowerCase() + "%";
    }

    private Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", "id", categoryId));
    }
}
