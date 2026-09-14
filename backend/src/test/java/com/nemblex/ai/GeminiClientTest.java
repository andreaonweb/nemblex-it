package com.nemblex.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.nemblex.entity.enums.TicketPriority;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GeminiClientTest {

    private static final String DEFAULT_EMPLOYEE_MESSAGE =
            "Un técnico o supervisor se pondrá en contacto contigo en breve para resolver esta incidencia.";

    private final GeminiClient geminiClient = new GeminiClient(
            "https://generativelanguage.googleapis.com/v1beta", "test-key", "gemini-3.6-flash",
            "gemini-embedding-2", 768);

    @Test
    void parseModelOutput_shouldParsePlainJson() {
        String raw = "{\"category\": \"Redes\", \"priority\": \"HIGH\", \"reasoning\": \"Palabras clave de VPN\", "
                + "\"employeeMessage\": \"Esperá 15 minutos y volvé a intentar conectarte a la VPN.\"}";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).contains(new AiClassificationResult(
                "Redes", TicketPriority.HIGH, "Palabras clave de VPN",
                "Esperá 15 minutos y volvé a intentar conectarte a la VPN."));
    }

    @Test
    void parseModelOutput_shouldStripMarkdownJsonCodeFence() {
        String raw = "```json\n{\"category\": \"Hardware\", \"priority\": \"MEDIUM\", \"reasoning\": \"Fallo de disco\", "
                + "\"employeeMessage\": \"\"}\n```";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).contains(new AiClassificationResult(
                "Hardware", TicketPriority.MEDIUM, "Fallo de disco", DEFAULT_EMPLOYEE_MESSAGE));
    }

    @Test
    void parseModelOutput_shouldStripPlainCodeFenceWithoutLanguageTag() {
        String raw = "```\n{\"category\": \"Software\", \"priority\": \"LOW\", \"reasoning\": \"Actualizacion pendiente\", "
                + "\"employeeMessage\": \"\"}\n```";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).contains(new AiClassificationResult(
                "Software", TicketPriority.LOW, "Actualizacion pendiente", DEFAULT_EMPLOYEE_MESSAGE));
    }

    @Test
    void parseModelOutput_shouldReturnEmpty_whenNotValidJson() {
        Optional<AiClassificationResult> result = geminiClient.parseModelOutput("no soy json");

        assertThat(result).isEmpty();
    }

    @Test
    void parseModelOutput_shouldReturnEmpty_whenMissingRequiredField() {
        String raw = "{\"category\": \"Redes\", \"priority\": \"HIGH\"}";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void parseModelOutput_shouldReturnEmpty_whenPriorityIsNotAValidEnumValue() {
        String raw = "{\"category\": \"Redes\", \"priority\": \"URGENTE\", \"reasoning\": \"algo\", \"employeeMessage\": \"\"}";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void parseModelOutput_shouldFallBackToDefaultEmployeeMessage_whenFieldIsMissing() {
        String raw = "{\"category\": \"Redes\", \"priority\": \"HIGH\", \"reasoning\": \"algo\"}";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).isPresent();
        assertThat(result.get().employeeMessage()).isEqualTo(DEFAULT_EMPLOYEE_MESSAGE);
    }

    @Test
    void parseModelOutput_shouldFallBackToDefaultEmployeeMessage_whenFieldIsBlank() {
        String raw = "{\"category\": \"Redes\", \"priority\": \"HIGH\", \"reasoning\": \"algo\", \"employeeMessage\": \"   \"}";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).isPresent();
        assertThat(result.get().employeeMessage()).isEqualTo(DEFAULT_EMPLOYEE_MESSAGE);
    }

    @Test
    void parseEmbeddingResponse_shouldParseValuesArray() {
        String raw = "{\"embedding\": {\"values\": [0.1, -0.2, 0.3]}}";

        Optional<float[]> result = geminiClient.parseEmbeddingResponse(raw);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(0.1f, -0.2f, 0.3f);
    }

    @Test
    void parseEmbeddingResponse_shouldReturnEmpty_whenNotValidJson() {
        Optional<float[]> result = geminiClient.parseEmbeddingResponse("no soy json");

        assertThat(result).isEmpty();
    }

    @Test
    void parseEmbeddingResponse_shouldReturnEmpty_whenValuesFieldMissing() {
        String raw = "{\"embedding\": {}}";

        Optional<float[]> result = geminiClient.parseEmbeddingResponse(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void extractActionProposal_shouldParseAction_whenModelCallsProposeActionTool() {
        String raw = """
                {"candidates": [{"content": {"parts": [
                    {"functionCall": {"name": "proposeAction", "args": {"action": "CLOSE", "reason": "Ya existe el ticket #8", \
                "employeeMessage": "Ya identificamos este problema, no necesitás hacer nada mas."}}}
                ]}}]}""";

        Optional<ActionProposal> result = geminiClient.extractActionProposal(raw);

        assertThat(result).contains(new ActionProposal(
                "CLOSE", "Ya existe el ticket #8", "Ya identificamos este problema, no necesitás hacer nada mas."));
    }

    @Test
    void extractActionProposal_shouldFallBackToDefaultEmployeeMessage_whenFieldIsBlank() {
        String raw = """
                {"candidates": [{"content": {"parts": [
                    {"functionCall": {"name": "proposeAction", "args": {"action": "ESCALATE", "reason": "afecta a toda la planta", \
                "employeeMessage": ""}}}
                ]}}]}""";

        Optional<ActionProposal> result = geminiClient.extractActionProposal(raw);

        assertThat(result).isPresent();
        assertThat(result.get().employeeMessage()).isEqualTo(DEFAULT_EMPLOYEE_MESSAGE);
    }

    @Test
    void extractActionProposal_shouldFallBackToDefaultEmployeeMessage_whenFieldIsMissing() {
        String raw = """
                {"candidates": [{"content": {"parts": [
                    {"functionCall": {"name": "proposeAction", "args": {"action": "ESCALATE", "reason": "afecta a toda la planta"}}}
                ]}}]}""";

        Optional<ActionProposal> result = geminiClient.extractActionProposal(raw);

        assertThat(result).isPresent();
        assertThat(result.get().employeeMessage()).isEqualTo(DEFAULT_EMPLOYEE_MESSAGE);
    }

    @Test
    void extractActionProposal_shouldReturnEmpty_whenActionIsNone() {
        String raw = """
                {"candidates": [{"content": {"parts": [
                    {"functionCall": {"name": "proposeAction", "args": {"action": "NONE", "reason": "sin evidencia suficiente"}}}
                ]}}]}""";

        Optional<ActionProposal> result = geminiClient.extractActionProposal(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void extractActionProposal_shouldReturnEmpty_whenModelDidNotCallAnyTool() {
        String raw = """
                {"candidates": [{"content": {"parts": [
                    {"text": "No hace falta ninguna accion."}
                ]}}]}""";

        Optional<ActionProposal> result = geminiClient.extractActionProposal(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void extractActionProposal_shouldReturnEmpty_whenFunctionCallIsForADifferentTool() {
        String raw = """
                {"candidates": [{"content": {"parts": [
                    {"functionCall": {"name": "otraTool", "args": {"action": "CLOSE", "reason": "x"}}}
                ]}}]}""";

        Optional<ActionProposal> result = geminiClient.extractActionProposal(raw);

        assertThat(result).isEmpty();
    }

    @Test
    void extractActionProposal_shouldReturnEmpty_whenResponseBodyIsNotValidJson() {
        Optional<ActionProposal> result = geminiClient.extractActionProposal("no soy json");

        assertThat(result).isEmpty();
    }
}
