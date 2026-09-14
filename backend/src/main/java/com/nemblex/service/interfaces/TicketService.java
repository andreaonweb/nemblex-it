package com.nemblex.service.interfaces;

import com.nemblex.dto.request.TicketRequest;
import com.nemblex.dto.request.TicketUpdateRequest;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.entity.enums.TicketStatus;
import java.util.List;

public interface TicketService {

    TicketResponse createTicket(TicketRequest request, Long creatorId);

    List<TicketResponse> getAllTickets(TicketStatus status, Long categoryId);

    TicketResponse getTicketById(Long id);

    TicketResponse updateTicket(Long id, TicketUpdateRequest request);

    void deleteTicket(Long id);

    TicketResponse assignToMe(Long id, Long userId);

    TicketResponse unassign(Long id, Long userId);

    List<TicketResponse> getMyTickets(Long userId);
}
