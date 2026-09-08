package com.nemblex.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemblex.entity.enums.TicketPriority;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String SYSTEM_PROMPT = """
            Eres un asistente de clasificacion de incidencias IT. A partir del titulo y la \
            descripcion de una incidencia, propone una categoria breve y una prioridad. Si se \
            te proporciona contexto interno (documentacion de procedimientos), basa tu \
            respuesta en ese contexto cuando sea relevante para el caso, en vez de usar solo \
            conocimiento general. Responde UNICAMENTE con un objeto JSON, sin texto adicional \
            ni bloques de codigo markdown, con este formato exacto: \
            {"category": "<categoria breve>", "priority": "LOW|MEDIUM|HIGH", "reasoning": "<explicacion breve>"}""";

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
        String prompt = buildPrompt(title, description, context);
        Map<String, Object> requestBody =
                Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

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

            return extractModelText(responseBody).flatMap(this::parseModelOutput);
        } catch (Exception ex) {
            log.warn("Gemini classification call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(String title, String description, List<String> context) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
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

    private Optional<String> extractModelText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            return textNode.isMissingNode() ? Optional.empty() : Optional.of(textNode.asText());
        } catch (Exception ex) {
            log.warn("Could not read Gemini response envelope: {}", ex.getMessage());
            return Optional.empty();
        }
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
