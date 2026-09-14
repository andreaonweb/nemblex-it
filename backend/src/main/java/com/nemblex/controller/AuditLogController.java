package com.nemblex.controller;

import com.nemblex.dto.request.AuditLogApprovalRequest;
import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.service.interfaces.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Registra una accion de auditoria sobre una incidencia en estado PENDING")
    @PostMapping
    public ResponseEntity<AuditLogResponse> create(@Valid @RequestBody AuditLogRequest request) {
        AuditLogResponse created = auditLogService.createLog(request);
        return ResponseEntity
                .created(URI.create("/api/audit-logs/" + created.getId()))
                .body(created);
    }

    @Operation(summary = "Resuelve una incidencia de inmediato con una accion manual del tecnico, sin pasar por aprobacion")
    @PostMapping("/resolve-now")
    public ResponseEntity<AuditLogResponse> resolveNow(@Valid @RequestBody AuditLogRequest request,
                                                        Authentication authentication) {
        AuditLogResponse resolved = auditLogService.resolveDirectly(request, currentUserId(authentication));
        return ResponseEntity
                .created(URI.create("/api/audit-logs/" + resolved.getId()))
                .body(resolved);
    }

    @Operation(summary = "Lista los registros de auditoria de una incidencia concreta")
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<AuditLogResponse>> getByTicket(@PathVariable Long ticketId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(auditLogService.getLogsByTicket(ticketId, currentUser(authentication)));
    }

    @Operation(summary = "Lista los registros de auditoria pendientes de aprobacion (SUPERVISOR o ADMIN)")
    @GetMapping("/pending")
    public ResponseEntity<List<AuditLogResponse>> getPending() {
        return ResponseEntity.ok(auditLogService.getAllPending());
    }

    @Operation(summary = "Aprueba o rechaza un registro de auditoria pendiente (SUPERVISOR o ADMIN)")
    @PutMapping("/{id}/resolve")
    public ResponseEntity<AuditLogResponse> resolve(@PathVariable Long id,
                                                    @Valid @RequestBody AuditLogApprovalRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(
                auditLogService.resolveLog(id, request, currentUserId(authentication)));
    }

    @Operation(summary = "Deshace la resolucion de un registro de auditoria, volviendolo a PENDING (SUPERVISOR o ADMIN)")
    @PutMapping("/{id}/undo")
    public ResponseEntity<AuditLogResponse> undo(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.undoResolution(id));
    }

    private Long currentUserId(Authentication authentication) {
        return currentUser(authentication).getId();
    }

    private AppUser currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
