package com.nemblex.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nemblex.ai.AiClassificationResult;
import com.nemblex.ai.GeminiClient;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.AuditResultStatus;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.TicketRepository;
import com.nemblex.service.interfaces.AuditLogService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketAiServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TicketAiServiceImpl ticketAiService;

    @Test
    void classifyTicket_shouldCreateAiClassifyLog_whenGeminiReturnsValidResult() {
        // Arrange
        Ticket ticket = Ticket.builder()
                .id(1L)
                .title("VPN no conecta")
                .description("Los usuarios no pueden conectarse a la VPN corporativa")
                .build();
        AiClassificationResult classification =
                new AiClassificationResult("Redes", TicketPriority.HIGH, "Palabras clave de VPN detectadas");
        AuditLogResponse expectedResponse = AuditLogResponse.builder()
                .id(40L)
                .ticketId(1L)
                .action("AI_CLASSIFY")
                .reason("Palabras clave de VPN detectadas")
                .resultStatus(AuditResultStatus.PENDING)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.classifyTicket("VPN no conecta", "Los usuarios no pueden conectarse a la VPN corporativa"))
                .thenReturn(Optional.of(classification));
        when(auditLogService.createAiClassification(1L, "Palabras clave de VPN detectadas"))
                .thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = ticketAiService.classifyTicket(1L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(auditLogService).createAiClassification(1L, "Palabras clave de VPN detectadas");
    }

    @Test
    void classifyTicket_shouldThrowResourceNotFoundException_whenTicketNotExists() {
        // Arrange
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketAiService.classifyTicket(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogService, never()).createAiClassification(anyLong(), anyString());
    }

    @Test
    void classifyTicket_shouldThrowBadRequestException_whenGeminiCannotClassify() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).title("t").description("d").build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.classifyTicket("t", "d")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketAiService.classifyTicket(1L))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogService, never()).createAiClassification(anyLong(), anyString());
    }
}
