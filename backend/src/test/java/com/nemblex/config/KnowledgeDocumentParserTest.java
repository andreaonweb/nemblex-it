package com.nemblex.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nemblex.config.KnowledgeDocumentParser.SeedDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentParserTest {

    @Test
    void parse_shouldSplitMultipleDocumentsByHeader() {
        String raw = """
                === DOCUMENTO 1: Acceso VPN y credenciales ===

                Primera linea del documento 1.
                Segunda linea del documento 1.


                === DOCUMENTO 2: Impresoras de red compartidas ===

                Contenido del documento 2.
                """;

        List<SeedDocument> result = KnowledgeDocumentParser.parse(raw);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Acceso VPN y credenciales");
        assertThat(result.get(0).content()).isEqualTo("Primera linea del documento 1.\nSegunda linea del documento 1.");
        assertThat(result.get(1).title()).isEqualTo("Impresoras de red compartidas");
        assertThat(result.get(1).content()).isEqualTo("Contenido del documento 2.");
    }

    @Test
    void parse_shouldReturnEmptyList_whenNoHeadersPresent() {
        List<SeedDocument> result = KnowledgeDocumentParser.parse("solo texto suelto sin cabeceras");

        assertThat(result).isEmpty();
    }

    @Test
    void parse_shouldTrimTitleAndContentWhitespace() {
        String raw = "===   DOCUMENTO 7:   Titulo con espacios   ===\n\n   contenido con espacios   \n\n";

        List<SeedDocument> result = KnowledgeDocumentParser.parse(raw);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Titulo con espacios");
        assertThat(result.get(0).content()).isEqualTo("contenido con espacios");
    }
}
