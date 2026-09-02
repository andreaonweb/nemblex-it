package com.nemblex.repository;

import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.TicketStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByCategoryId(Long categoryId);
}
