package com.nemblex.service.interfaces;

import com.nemblex.dto.request.TicketRequest;
import com.nemblex.dto.request.TicketUpdateRequest;
import com.nemblex.dto.response.PagedResponse;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.dto.response.TicketStatsResponse;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;
import org.springframework.data.domain.Pageable;

public interface TicketService {

    TicketResponse createTicket(TicketRequest request, Long creatorId);

    PagedResponse<TicketResponse> getAllTickets(TicketStatus status, TicketPriority priority, Long categoryId,
                                                 String search, Pageable pageable);

    TicketStatsResponse getStats();

    TicketResponse getTicketById(Long id);

    TicketResponse updateTicket(Long id, TicketUpdateRequest request);

    void deleteTicket(Long id);

    TicketResponse assignToMe(Long id, Long userId);

    TicketResponse unassign(Long id, Long userId);

    PagedResponse<TicketResponse> getMyTickets(Long userId, TicketStatus status, TicketPriority priority,
                                                String search, Pageable pageable);

    TicketStatsResponse getMyStats(Long userId);
}
