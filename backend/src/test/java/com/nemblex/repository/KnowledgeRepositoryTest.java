package com.nemblex.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KnowledgeRepositoryTest {

    @Test
    void toVectorLiteral_shouldFormatAsPgvectorTextLiteral() {
        String literal = KnowledgeRepository.toVectorLiteral(new float[] {0.1f, -0.25f, 3f});

        assertThat(literal).isEqualTo("[0.1,-0.25,3.0]");
    }

    @Test
    void toVectorLiteral_shouldHandleSingleElement() {
        String literal = KnowledgeRepository.toVectorLiteral(new float[] {1.5f});

        assertThat(literal).isEqualTo("[1.5]");
    }
}
