package com.nemblex.repository;

import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByCategoryId(Long categoryId);

    List<Ticket> findByCreatedById(Long createdById);

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:createdById IS NULL OR t.createdBy.id = :createdById)
              AND (:search IS NULL
                   OR LOWER(t.title) LIKE :search
                   OR LOWER(t.createdBy.name) LIKE :search)
            """)
    Page<Ticket> search(@Param("status") TicketStatus status,
                        @Param("priority") TicketPriority priority,
                        @Param("categoryId") Long categoryId,
                        @Param("createdById") Long createdById,
                        @Param("search") String search,
                        Pageable pageable);

    @Query("""
            SELECT t.status AS status, t.priority AS priority, COUNT(t) AS count
            FROM Ticket t
            WHERE (:createdById IS NULL OR t.createdBy.id = :createdById)
            GROUP BY t.status, t.priority
            """)
    List<TicketStatusPriorityCount> countByStatusAndPriority(@Param("createdById") Long createdById);
}
