package com.nemblex.service.impl;

import com.nemblex.dto.request.TicketRequest;
import com.nemblex.dto.request.TicketUpdateRequest;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.Category;
import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.mapper.TicketMapper;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.repository.CategoryRepository;
import com.nemblex.repository.TicketRepository;
import com.nemblex.service.interfaces.TicketService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             AppUserRepository userRepository,
                             CategoryRepository categoryRepository,
                             TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public TicketResponse createTicket(TicketRequest request) {
        AppUser creator = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getCreatedBy()));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setCreatedBy(creator);
        ticket.setCategory(resolveCategory(request.getCategoryId()));

        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(TicketStatus status, Long categoryId) {
        List<Ticket> tickets;
        if (status != null && categoryId != null) {
            tickets = ticketRepository.findByStatus(status).stream()
                    .filter(t -> t.getCategory() != null && categoryId.equals(t.getCategory().getId()))
                    .toList();
        } else if (status != null) {
            tickets = ticketRepository.findByStatus(status);
        } else if (categoryId != null) {
            tickets = ticketRepository.findByCategoryId(categoryId);
        } else {
            tickets = ticketRepository.findAll();
        }
        return tickets.stream().map(ticketMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        return ticketMapper.toResponse(findTicketOrThrow(id));
    }

    @Override
    public TicketResponse updateTicket(Long id, TicketUpdateRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        ticketMapper.updateFromRequest(request, ticket);

        if (request.getCategoryId() != null) {
            ticket.setCategory(resolveCategory(request.getCategoryId()));
        }
        if (request.getAssignedTo() != null) {
            ticket.setAssignedTo(userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedTo())));
        }

        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public void deleteTicket(Long id) {
        ticketRepository.delete(findTicketOrThrow(id));
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
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }
}
