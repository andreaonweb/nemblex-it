package com.nemblex.controller;

import com.nemblex.dto.request.TicketRequest;
import com.nemblex.dto.request.TicketUpdateRequest;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.enums.TicketStatus;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.service.interfaces.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
    private final AppUserRepository userRepository;

    public TicketController(TicketService ticketService, AppUserRepository userRepository) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Crea una nueva incidencia")
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request,
                                                  Authentication authentication) {
        TicketResponse created = ticketService.createTicket(request, currentUserId(authentication));
        return ResponseEntity
                .created(URI.create("/api/tickets/" + created.getId()))
                .body(created);
    }

    @Operation(summary = "Lista incidencias, con filtro opcional por estado y categoria")
    @GetMapping
    public ResponseEntity<List<TicketResponse>> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ticketService.getAllTickets(status, categoryId));
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

    @Operation(summary = "Elimina una incidencia")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return user.getId();
    }
}
