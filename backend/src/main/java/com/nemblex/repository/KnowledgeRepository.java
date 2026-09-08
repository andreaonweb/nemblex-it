package com.nemblex.repository;

import com.nemblex.entity.KnowledgeDocument;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Long.class);
        return count == null ? 0L : count;
    }

    public void save(String title, String content, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO knowledge_document (title, content, embedding) VALUES (?, ?, ?::vector)",
                title, content, toVectorLiteral(embedding));
    }

    public List<KnowledgeDocument> findNearest(float[] queryEmbedding, int limit) {
        return jdbcTemplate.query(
                "SELECT id, title, content FROM knowledge_document ORDER BY embedding <=> ?::vector LIMIT ?",
                (rs, rowNum) -> new KnowledgeDocument(rs.getLong("id"), rs.getString("title"), rs.getString("content")),
                toVectorLiteral(queryEmbedding), limit);
    }

    static String toVectorLiteral(float[] embedding) {
        StringBuilder literal = new StringBuilder(embedding.length * 8 + 2);
        literal.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(embedding[i]);
        }
        literal.append(']');
        return literal.toString();
    }
}
