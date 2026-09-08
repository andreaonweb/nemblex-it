package com.nemblex.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nemblex.controller.TicketController;
import com.nemblex.dto.response.TicketResponse;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.enums.TicketStatus;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.service.interfaces.TicketAiService;
import com.nemblex.service.interfaces.TicketService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
@Import(SecurityConfig.class)
class EmployeeRoleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketAiService ticketAiService;

    @MockitoBean
    private AppUserRepository userRepository;

    @MockitoBean
    private CustomAuthenticationManager authenticationManager;

    @Test
    @WithMockUser(username = "carlos.mendez@nemblex.dev", roles = "EMPLOYEE")
    void employee_canCreateTicket() throws Exception {
        when(userRepository.findByEmail("carlos.mendez@nemblex.dev"))
                .thenReturn(Optional.of(AppUser.builder().id(9L).build()));
        when(ticketService.createTicket(any(), anyLong()))
                .thenReturn(TicketResponse.builder().id(1L).status(TicketStatus.NEW).build());

        mockMvc.perform(post("/api/tickets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Necesito acceso a la VPN\",\"description\":\"No puedo conectarme\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "carlos.mendez@nemblex.dev", roles = "EMPLOYEE")
    void employee_canListOwnTickets() throws Exception {
        when(userRepository.findByEmail("carlos.mendez@nemblex.dev"))
                .thenReturn(Optional.of(AppUser.builder().id(9L).build()));
        when(ticketService.getMyTickets(9L)).thenReturn(List.of());

        mockMvc.perform(get("/api/tickets/mine"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_isForbiddenFromFullTicketList() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_isForbiddenFromAuditLogsCreate() throws Exception {
        mockMvc.perform(post("/api/audit-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":1,\"action\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_isForbiddenFromPendingApprovals() throws Exception {
        mockMvc.perform(get("/api/audit-logs/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_isForbiddenFromAssignToMe() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/tickets/1/assign-to-me").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technician_canStillListFullTicketList() throws Exception {
        when(ticketService.getAllTickets(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk());
    }
}
