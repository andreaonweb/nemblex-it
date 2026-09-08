package com.nemblex.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.nemblex.entity.enums.TicketPriority;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GeminiClientTest {

    private final GeminiClient geminiClient = new GeminiClient(
            "https://generativelanguage.googleapis.com/v1beta", "test-key", "gemini-3.6-flash",
            "gemini-embedding-2", 768);

    @Test
    void parseModelOutput_shouldParsePlainJson() {
        String raw = "{\"category\": \"Redes\", \"priority\": \"HIGH\", \"reasoning\": \"Palabras clave de VPN\"}";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).contains(new AiClassificationResult("Redes", TicketPriority.HIGH, "Palabras clave de VPN"));
    }

    @Test
    void parseModelOutput_shouldStripMarkdownJsonCodeFence() {
        String raw = "```json\n{\"category\": \"Hardware\", \"priority\": \"MEDIUM\", \"reasoning\": \"Fallo de disco\"}\n```";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).contains(new AiClassificationResult("Hardware", TicketPriority.MEDIUM, "Fallo de disco"));
    }

    @Test
    void parseModelOutput_shouldStripPlainCodeFenceWithoutLanguageTag() {
        String raw = "```\n{\"category\": \"Software\", \"priority\": \"LOW\", \"reasoning\": \"Actualizacion pendiente\"}\n```";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).contains(new AiClassificationResult("Software", TicketPriority.LOW, "Actualizacion pendiente"));
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
        String raw = "{\"category\": \"Redes\", \"priority\": \"URGENTE\", \"reasoning\": \"algo\"}";

        Optional<AiClassificationResult> result = geminiClient.parseModelOutput(raw);

        assertThat(result).isEmpty();
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
                    {"functionCall": {"name": "proposeAction", "args": {"action": "CLOSE", "reason": "Ya existe el ticket #8"}}}
                ]}}]}""";

        Optional<ActionProposal> result = geminiClient.extractActionProposal(raw);

        assertThat(result).contains(new ActionProposal("CLOSE", "Ya existe el ticket #8"));
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
