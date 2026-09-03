package com.nemblex.controller;

import com.nemblex.dto.request.AuditLogApprovalRequest;
import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.service.interfaces.AuditLogService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AppUserRepository userRepository;

    public AuditLogController(AuditLogService auditLogService, AppUserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<AuditLogResponse> create(@Valid @RequestBody AuditLogRequest request) {
        AuditLogResponse created = auditLogService.createLog(request);
        return ResponseEntity
                .created(URI.create("/api/audit-logs/" + created.getId()))
                .body(created);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<AuditLogResponse>> getByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(auditLogService.getLogsByTicket(ticketId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AuditLogResponse>> getPending() {
        return ResponseEntity.ok(auditLogService.getAllPending());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<AuditLogResponse> resolve(@PathVariable Long id,
                                                    @Valid @RequestBody AuditLogApprovalRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(
                auditLogService.resolveLog(id, request, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return user.getId();
    }
}
