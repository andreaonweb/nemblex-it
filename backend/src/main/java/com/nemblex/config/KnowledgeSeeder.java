package com.nemblex.config;

import com.nemblex.ai.GeminiClient;
import com.nemblex.config.KnowledgeDocumentParser.SeedDocument;
import com.nemblex.repository.KnowledgeRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
@Profile("dev")
public class KnowledgeSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSeeder.class);
    private static final String SOURCE_PATH = "knowledge/nemblex_docs_rag.txt";

    private final KnowledgeRepository knowledgeRepository;
    private final GeminiClient geminiClient;

    public KnowledgeSeeder(KnowledgeRepository knowledgeRepository, GeminiClient geminiClient) {
        this.knowledgeRepository = knowledgeRepository;
        this.geminiClient = geminiClient;
    }

    @Override
    public void run(String... args) {
        if (knowledgeRepository.count() > 0) {
            log.info("knowledge_document ya tiene datos, se omite el seeding");
            return;
        }

        List<SeedDocument> documents = KnowledgeDocumentParser.parse(readSourceFile());
        for (SeedDocument document : documents) {
            geminiClient.embedText(document.title() + "\n\n" + document.content()).ifPresentOrElse(
                    embedding -> {
                        knowledgeRepository.save(document.title(), document.content(), embedding);
                        log.info("Documento de conocimiento indexado: {}", document.title());
                    },
                    () -> log.warn("No se pudo generar el embedding para: {}", document.title()));
        }
    }

    private String readSourceFile() {
        try (InputStream in = new ClassPathResource(SOURCE_PATH).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer " + SOURCE_PATH, ex);
        }
    }
}
