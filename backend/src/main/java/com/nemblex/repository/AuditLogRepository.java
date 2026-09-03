package com.nemblex.repository;

import com.nemblex.entity.AuditLog;
import com.nemblex.entity.enums.AuditResultStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTicketId(Long ticketId);

    List<AuditLog> findByResultStatus(AuditResultStatus resultStatus);
}
