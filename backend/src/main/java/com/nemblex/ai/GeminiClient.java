package com.nemblex.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemblex.entity.enums.TicketPriority;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String CLASSIFICATION_PROMPT = """
            Eres un asistente de clasificacion de incidencias IT. A partir del titulo y la \
            descripcion de una incidencia, propone una categoria breve y una prioridad. Si se \
            te proporciona contexto interno (documentacion de procedimientos), basa tu \
            respuesta en ese contexto cuando sea relevante para el caso, en vez de usar solo \
            conocimiento general. Responde UNICAMENTE con un objeto JSON, sin texto adicional \
            ni bloques de codigo markdown, con este formato exacto: \
            {"category": "<categoria breve>", "priority": "LOW|MEDIUM|HIGH", "reasoning": "<explicacion breve>"}""";

    private static final String ACTION_PROMPT = """
            Eres un asistente que decide si corresponde proponer una accion concreta sobre una \
            incidencia de IT, a partir de su titulo, descripcion, y el contexto interno \
            (documentacion de procedimientos) cuando este disponible. Vos NO ejecutas ninguna \
            accion: solo la propones invocando la tool proposeAction, y un supervisor humano \
            decidira despues si la aprueba o la rechaza desde la cola de aprobaciones. \
            Ejemplos basados en el contexto interno: si el ticket es un duplicado claro de \
            otro ya abierto para el mismo problema o activo, propone CLOSE; si el problema \
            afecta a varios usuarios o a un departamento entero de forma simultanea (no un \
            caso individual), propone ESCALATE; si el caso esta fuera del alcance de un \
            tecnico de Nivel 1 y requiere un equipo especializado, propone REASSIGN. Si no hay \
            evidencia clara para ninguna de esas acciones, no invoques ninguna tool.""";

    private static final Set<String> VALID_ACTIONS = Set.of("CLOSE", "ESCALATE", "REASSIGN");

    private static final Map<String, Object> PROPOSE_ACTION_TOOL = Map.of(
            "functionDeclarations", List.of(Map.of(
                    "name", "proposeAction",
                    "description",
                    "Propone una accion concreta sobre la incidencia para que un supervisor humano la "
                            + "apruebe o la rechace. No ejecuta la accion.",
                    "parameters", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "action", Map.of(
                                            "type", "STRING",
                                            "enum", List.of("CLOSE", "ESCALATE", "REASSIGN", "NONE"),
                                            "description",
                                            "CLOSE si es un duplicado, ESCALATE si afecta a varios usuarios o "
                                                    + "un departamento entero, REASSIGN si esta fuera del "
                                                    + "alcance de Nivel 1, NONE si no hay accion clara."),
                                    "reason", Map.of(
                                            "type", "STRING",
                                            "description", "Justificacion de la accion propuesta, basada en el "
                                                    + "ticket y el contexto interno.")),
                            "required", List.of("action", "reason")))));

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String embeddingModel;
    private final int embeddingDimension;

    public GeminiClient(@Value("${app.ai.gemini.base-url}") String baseUrl,
                         @Value("${app.ai.gemini.api-key}") String apiKey,
                         @Value("${app.ai.gemini.model}") String model,
                         @Value("${app.ai.gemini.embedding-model}") String embeddingModel,
                         @Value("${app.ai.gemini.embedding-dimension}") int embeddingDimension) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
    }

    public Optional<AiClassificationResult> classifyTicket(String title, String description, List<String> context) {
        Optional<AiClassificationResult> classification = requestClassification(title, description, context);
        if (classification.isEmpty()) {
            return Optional.empty();
        }

        ActionProposal proposal = requestActionProposal(title, description, context).orElse(null);
        AiClassificationResult base = classification.get();
        return Optional.of(new AiClassificationResult(base.category(), base.priority(), base.reasoning(), proposal));
    }

    private Optional<AiClassificationResult> requestClassification(String title, String description, List<String> context) {
        String prompt = buildPrompt(CLASSIFICATION_PROMPT, title, description, context);
        Map<String, Object> requestBody =
                Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        Optional<String> responseBody = callGenerateContent(requestBody, "classification");
        return responseBody.flatMap(this::extractClassificationText).flatMap(this::parseModelOutput);
    }

    private Optional<ActionProposal> requestActionProposal(String title, String description, List<String> context) {
        String prompt = buildPrompt(ACTION_PROMPT, title, description, context);
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "tools", List.of(PROPOSE_ACTION_TOOL));

        Optional<String> responseBody = callGenerateContent(requestBody, "action proposal");
        return responseBody.flatMap(this::extractActionProposal);
    }

    private Optional<String> callGenerateContent(Map<String, Object> requestBody, String callDescription) {
        try {
            String responseBody = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(45))
                    .block();
            return Optional.ofNullable(responseBody);
        } catch (Exception ex) {
            log.warn("Gemini {} call failed: {}", callDescription, ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(String systemPrompt, String title, String description, List<String> context) {
        StringBuilder prompt = new StringBuilder(systemPrompt);
        if (context != null && !context.isEmpty()) {
            prompt.append("\n\nContexto interno relevante:");
            for (String snippet : context) {
                prompt.append("\n---\n").append(snippet);
            }
            prompt.append("\n---");
        }
        prompt.append("\n\nTitulo: ").append(title).append("\nDescripcion: ").append(description);
        return prompt.toString();
    }

    public Optional<float[]> embedText(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", embeddingDimension);

        try {
            String responseBody = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:embedContent")
                            .queryParam("key", apiKey)
                            .build(embeddingModel))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            return parseEmbeddingResponse(responseBody);
        } catch (Exception ex) {
            log.warn("Gemini embedding call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractClassificationText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            return textNode.isMissingNode() ? Optional.empty() : Optional.of(textNode.asText());
        } catch (Exception ex) {
            log.warn("Could not read Gemini classification response envelope: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    Optional<ActionProposal> extractActionProposal(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode partsNode = root.at("/candidates/0/content/parts");
            if (!partsNode.isArray()) {
                return Optional.empty();
            }

            for (JsonNode part : partsNode) {
                JsonNode functionCallNode = part.get("functionCall");
                if (functionCallNode != null && "proposeAction".equals(functionCallNode.path("name").asText(null))) {
                    return Optional.ofNullable(parseActionProposal(functionCallNode.path("args")));
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Could not parse Gemini action proposal response: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private ActionProposal parseActionProposal(JsonNode args) {
        String action = args.path("action").asText(null);
        String reason = args.path("reason").asText(null);
        if (action == null || reason == null) {
            return null;
        }
        String normalized = action.trim().toUpperCase();
        if (!VALID_ACTIONS.contains(normalized)) {
            return null;
        }
        return new ActionProposal(normalized, reason);
    }

    Optional<AiClassificationResult> parseModelOutput(String rawText) {
        try {
            String json = stripCodeFence(rawText);
            JsonNode node = objectMapper.readTree(json);

            String category = node.path("category").asText(null);
            String priorityRaw = node.path("priority").asText(null);
            String reasoning = node.path("reasoning").asText(null);

            if (category == null || priorityRaw == null || reasoning == null) {
                return Optional.empty();
            }

            TicketPriority priority = TicketPriority.valueOf(priorityRaw.trim().toUpperCase());
            return Optional.of(new AiClassificationResult(category, priority, reasoning));
        } catch (Exception ex) {
            log.warn("Could not parse Gemini classification output: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    Optional<float[]> parseEmbeddingResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode valuesNode = root.at("/embedding/values");
            if (!valuesNode.isArray() || valuesNode.isEmpty()) {
                return Optional.empty();
            }

            float[] vector = new float[valuesNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) valuesNode.get(i).asDouble();
            }
            return Optional.of(vector);
        } catch (Exception ex) {
            log.warn("Could not parse Gemini embedding response: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
