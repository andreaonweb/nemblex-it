package com.nemblex.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KnowledgeDocumentParser {

    private static final Pattern HEADER = Pattern.compile("===\\s*DOCUMENTO\\s+\\d+\\s*:\\s*(.+?)\\s*===");

    private KnowledgeDocumentParser() {
    }

    public record SeedDocument(String title, String content) {
    }

    public static List<SeedDocument> parse(String rawText) {
        List<SeedDocument> documents = new ArrayList<>();
        Matcher matcher = HEADER.matcher(rawText);

        List<int[]> headerBounds = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            headerBounds.add(new int[] {matcher.start(), matcher.end()});
            titles.add(matcher.group(1).trim());
        }

        for (int i = 0; i < headerBounds.size(); i++) {
            int contentStart = headerBounds.get(i)[1];
            int contentEnd = (i + 1 < headerBounds.size()) ? headerBounds.get(i + 1)[0] : rawText.length();
            String content = rawText.substring(contentStart, contentEnd).trim();
            documents.add(new SeedDocument(titles.get(i), content));
        }

        return documents;
    }
}
