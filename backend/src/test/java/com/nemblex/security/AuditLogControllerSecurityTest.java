package com.nemblex.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nemblex.controller.AuditLogController;
import com.nemblex.entity.AppUser;
import com.nemblex.entity.enums.Role;
import com.nemblex.repository.AppUserRepository;
import com.nemblex.service.interfaces.AuditLogService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
class AuditLogControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private AppUserRepository userRepository;

    @MockitoBean
    private CustomAuthenticationManager authenticationManager;

    @Test
    @WithMockUser(username = "carlos.mendez@nemblex.dev", roles = "EMPLOYEE")
    void employee_canReachOwnTicketAuditLogsRoute() throws Exception {
        when(userRepository.findByEmail("carlos.mendez@nemblex.dev"))
                .thenReturn(Optional.of(AppUser.builder().id(9L).role(Role.EMPLOYEE).build()));
        when(auditLogService.getLogsByTicket(anyLong(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/audit-logs/ticket/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_isStillForbiddenFromPendingApprovals() throws Exception {
        mockMvc.perform(get("/api/audit-logs/pending"))
                .andExpect(status().isForbidden());
    }
}
