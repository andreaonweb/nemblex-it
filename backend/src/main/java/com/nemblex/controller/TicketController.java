package com.nemblex.controller;

import com.nemblex.dto.request.TicketRequest;
import com.nemblex.dto.request.TicketUpdateRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.dto.response.PagedResponse;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.dto.response.TicketStatsResponse;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.entity.enums.TicketStatus;
import com.nemblex.security.AuthenticatedUserResolver;
import com.nemblex.service.interfaces.TicketAiService;
import com.nemblex.service.interfaces.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketAiService ticketAiService;
    private final AuthenticatedUserResolver userResolver;

    public TicketController(TicketService ticketService, TicketAiService ticketAiService,
                            AuthenticatedUserResolver userResolver) {
        this.ticketService = ticketService;
        this.ticketAiService = ticketAiService;
        this.userResolver = userResolver;
    }

    @Operation(summary = "Crea una nueva incidencia")
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request,
                                                  Authentication authentication) {
        TicketResponse created = ticketService.createTicket(request, userResolver.resolveId(authentication));
        return ResponseEntity
                .created(URI.create("/api/tickets/" + created.getId()))
                .body(created);
    }

    @Operation(summary = "Lista incidencias paginadas, con filtro opcional por estado, prioridad, categoria y busqueda")
    @GetMapping
    public ResponseEntity<PagedResponse<TicketResponse>> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ticketService.getAllTickets(status, priority, categoryId, search, pageable));
    }

    @Operation(summary = "KPIs y conteos por estado de todas las incidencias (TECHNICIAN/SUPERVISOR/ADMIN)")
    @GetMapping("/stats")
    public ResponseEntity<TicketStatsResponse> stats() {
        return ResponseEntity.ok(ticketService.getStats());
    }

    @Operation(summary = "Lista paginada de las incidencias creadas por el usuario autenticado")
    @GetMapping("/mine")
    public ResponseEntity<PagedResponse<TicketResponse>> mine(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(ticketService.getMyTickets(
                userResolver.resolveId(authentication), status, priority, search, pageable));
    }

    @Operation(summary = "KPIs y conteos por estado de las incidencias del usuario autenticado")
    @GetMapping("/mine/stats")
    public ResponseEntity<TicketStatsResponse> myStats(Authentication authentication) {
        return ResponseEntity.ok(ticketService.getMyStats(userResolver.resolveId(authentication)));
    }

    @Operation(summary = "Obtiene una incidencia por su id")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @Operation(summary = "Actualiza una incidencia existente")
    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> update(@PathVariable Long id,
                                                 @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    @Operation(summary = "Asigna la incidencia al usuario autenticado")
    @PutMapping("/{id}/assign-to-me")
    public ResponseEntity<TicketResponse> assignToMe(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.assignToMe(id, userResolver.resolveId(authentication)));
    }

    @Operation(summary = "Libera la asignacion de la incidencia (el propio asignado, o un supervisor/admin)")
    @PutMapping("/{id}/unassign")
    public ResponseEntity<TicketResponse> unassign(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.unassign(id, userResolver.resolveId(authentication)));
    }

    @Operation(summary = "Clasifica una incidencia con IA (Gemini): propone categoria y prioridad como AuditLog pendiente")
    @PostMapping("/{id}/classify")
    public ResponseEntity<AuditLogResponse> classify(@PathVariable Long id) {
        return ResponseEntity.ok(ticketAiService.classifyTicket(id));
    }

    @Operation(summary = "Elimina una incidencia")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
