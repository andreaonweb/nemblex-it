package com.nemblex.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nemblex.ai.ActionProposal;
import com.nemblex.ai.AiClassificationResult;
import com.nemblex.ai.GeminiClient;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.KnowledgeDocument;
import com.nemblex.entity.Ticket;
import com.nemblex.entity.enums.AuditResultStatus;
import com.nemblex.entity.enums.TicketPriority;
import com.nemblex.exception.BadRequestException;
import com.nemblex.exception.ResourceNotFoundException;
import com.nemblex.repository.KnowledgeRepository;
import com.nemblex.repository.TicketRepository;
import com.nemblex.service.interfaces.AuditLogService;
import java.util.List;
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
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TicketAiServiceImpl ticketAiService;

    private static Ticket vpnTicket() {
        return Ticket.builder()
                .id(1L)
                .title("VPN no conecta")
                .description("Los usuarios no pueden conectarse a la VPN corporativa")
                .build();
    }

    private static AuditLogResponse auditLog(long id, String action, String reason) {
        return AuditLogResponse.builder()
                .id(id)
                .ticketId(1L)
                .action(action)
                .reason(reason)
                .resultStatus(AuditResultStatus.PENDING)
                .build();
    }

    @Test
    void classifyTicket_shouldCreateAiClassifyLog_whenGeminiReturnsValidResult() {
        // Arrange
        Ticket ticket = vpnTicket();
        AiClassificationResult classification = new AiClassificationResult(
                "Redes", TicketPriority.HIGH, "Palabras clave de VPN detectadas",
                "Esperá 15 minutos y volvé a intentar conectarte a la VPN.");
        AuditLogResponse expectedResponse = auditLog(40L, "AI_CLASSIFY", "Palabras clave de VPN detectadas");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.embedText(anyString())).thenReturn(Optional.empty());
        when(geminiClient.classifyTicket("VPN no conecta", "Los usuarios no pueden conectarse a la VPN corporativa", List.of()))
                .thenReturn(Optional.of(classification));
        when(auditLogService.createAiClassificationProposal(
                1L, "Palabras clave de VPN detectadas", "Esperá 15 minutos y volvé a intentar conectarte a la VPN.",
                "Redes", TicketPriority.HIGH))
                .thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = ticketAiService.classifyTicket(1L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(auditLogService).createAiClassificationProposal(
                1L, "Palabras clave de VPN detectadas", "Esperá 15 minutos y volvé a intentar conectarte a la VPN.",
                "Redes", TicketPriority.HIGH);
    }

    @Test
    void classifyTicket_shouldThrowResourceNotFoundException_whenTicketNotExists() {
        // Arrange
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketAiService.classifyTicket(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditLogService, never())
                .createAiClassificationProposal(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void classifyTicket_shouldThrowBadRequestException_whenGeminiCannotClassify() {
        // Arrange
        Ticket ticket = Ticket.builder().id(1L).title("t").description("d").build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.embedText(anyString())).thenReturn(Optional.empty());
        when(geminiClient.classifyTicket("t", "d", List.of())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketAiService.classifyTicket(1L))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogService, never())
                .createAiClassificationProposal(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void classifyTicket_shouldPassNearestDocumentsAsContext_whenEmbeddingSucceeds() {
        // Arrange
        Ticket ticket = vpnTicket();
        float[] embedding = {0.1f, 0.2f};
        List<KnowledgeDocument> nearest = List.of(
                new KnowledgeDocument(1L, "Acceso VPN y credenciales", "Contenido del documento 1"),
                new KnowledgeDocument(2L, "Impresoras de red compartidas", "Contenido del documento 2"));
        AiClassificationResult classification = new AiClassificationResult(
                "Redes", TicketPriority.HIGH, "Basado en el documento de VPN",
                "Cerrá sesión completamente del cliente VPN y volvé a autenticarte.");
        AuditLogResponse expectedResponse = auditLog(41L, "AI_CLASSIFY", "Basado en el documento de VPN");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.embedText("VPN no conecta\nLos usuarios no pueden conectarse a la VPN corporativa"))
                .thenReturn(Optional.of(embedding));
        when(knowledgeRepository.findNearest(embedding, 2)).thenReturn(nearest);
        when(geminiClient.classifyTicket(
                "VPN no conecta",
                "Los usuarios no pueden conectarse a la VPN corporativa",
                List.of("Contenido del documento 1", "Contenido del documento 2")))
                .thenReturn(Optional.of(classification));
        when(auditLogService.createAiClassificationProposal(
                1L, "Basado en el documento de VPN", "Cerrá sesión completamente del cliente VPN y volvé a autenticarte.",
                "Redes", TicketPriority.HIGH))
                .thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = ticketAiService.classifyTicket(1L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(knowledgeRepository).findNearest(embedding, 2);
    }

    @Test
    void classifyTicket_shouldClassifyWithoutContext_whenEmbeddingFails() {
        // Arrange
        Ticket ticket = vpnTicket();
        AiClassificationResult classification = new AiClassificationResult(
                "Redes", TicketPriority.HIGH, "Sin contexto adicional",
                "Un técnico o supervisor se pondrá en contacto contigo en breve para resolver esta incidencia.");
        AuditLogResponse expectedResponse = auditLog(42L, "AI_CLASSIFY", "Sin contexto adicional");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.embedText(anyString())).thenReturn(Optional.empty());
        when(geminiClient.classifyTicket(
                "VPN no conecta", "Los usuarios no pueden conectarse a la VPN corporativa", List.of()))
                .thenReturn(Optional.of(classification));
        when(auditLogService.createAiClassificationProposal(
                1L, "Sin contexto adicional",
                "Un técnico o supervisor se pondrá en contacto contigo en breve para resolver esta incidencia.",
                "Redes", TicketPriority.HIGH))
                .thenReturn(expectedResponse);

        // Act
        AuditLogResponse result = ticketAiService.classifyTicket(1L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(knowledgeRepository, never()).findNearest(any(), anyInt());
    }

    @Test
    void classifyTicket_shouldCreateSecondAuditLog_whenModelProposesAnAction() {
        // Arrange
        Ticket ticket = vpnTicket();
        ActionProposal proposal = new ActionProposal(
                "CLOSE", "Duplicado del ticket #8, mismo activo e incidencia",
                "Ya identificamos este problema, no necesitás hacer nada más.");
        AiClassificationResult classification = new AiClassificationResult(
                "Impresoras", TicketPriority.LOW, "Coincide con un ticket ya abierto",
                "Ya identificamos este problema, no necesitás hacer nada más.", proposal);
        AuditLogResponse classifyResponse = auditLog(50L, "AI_CLASSIFY", "Coincide con un ticket ya abierto");
        AuditLogResponse closeResponse = auditLog(51L, "CLOSE", "Duplicado del ticket #8, mismo activo e incidencia");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.embedText(anyString())).thenReturn(Optional.empty());
        when(geminiClient.classifyTicket(anyString(), anyString(), eq(List.of())))
                .thenReturn(Optional.of(classification));
        when(auditLogService.createAiClassificationProposal(
                1L, "Coincide con un ticket ya abierto", "Ya identificamos este problema, no necesitás hacer nada más.",
                "Impresoras", TicketPriority.LOW))
                .thenReturn(classifyResponse);
        when(auditLogService.createAiProposal(
                1L, "CLOSE", "Duplicado del ticket #8, mismo activo e incidencia",
                "Ya identificamos este problema, no necesitás hacer nada más."))
                .thenReturn(closeResponse);

        // Act
        AuditLogResponse result = ticketAiService.classifyTicket(1L);

        // Assert: the endpoint still returns the classification log
        assertThat(result).isEqualTo(classifyResponse);
        verify(auditLogService).createAiClassificationProposal(
                1L, "Coincide con un ticket ya abierto", "Ya identificamos este problema, no necesitás hacer nada más.",
                "Impresoras", TicketPriority.LOW);
        verify(auditLogService).createAiProposal(
                1L, "CLOSE", "Duplicado del ticket #8, mismo activo e incidencia",
                "Ya identificamos este problema, no necesitás hacer nada más.");
    }

    @Test
    void classifyTicket_shouldNotCreateSecondAuditLog_whenModelProposesNoAction() {
        // Arrange
        Ticket ticket = vpnTicket();
        AiClassificationResult classification = new AiClassificationResult(
                "Redes", TicketPriority.MEDIUM, "Caso ambiguo, sin accion clara",
                "Un técnico o supervisor se pondrá en contacto contigo en breve para resolver esta incidencia.", null);
        AuditLogResponse classifyResponse = auditLog(52L, "AI_CLASSIFY", "Caso ambiguo, sin accion clara");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(geminiClient.embedText(anyString())).thenReturn(Optional.empty());
        when(geminiClient.classifyTicket(anyString(), anyString(), eq(List.of())))
                .thenReturn(Optional.of(classification));
        when(auditLogService.createAiClassificationProposal(
                1L, "Caso ambiguo, sin accion clara",
                "Un técnico o supervisor se pondrá en contacto contigo en breve para resolver esta incidencia.",
                "Redes", TicketPriority.MEDIUM))
                .thenReturn(classifyResponse);

        // Act
        AuditLogResponse result = ticketAiService.classifyTicket(1L);

        // Assert
        assertThat(result).isEqualTo(classifyResponse);
        verify(auditLogService, never()).createAiProposal(anyLong(), anyString(), anyString(), anyString());
    }
}
