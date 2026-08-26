package com.gyubot.search.chunk;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
 * 문단 단위로 묶어가며 목표 크기(TARGET_CHUNK_SIZE)에 맞춰 청크를 만든다. 문단이 목표 크기보다
 * 길면 문자 단위 슬라이딩 윈도우로 쪼갠다. 각 청크 사이에 OVERLAP만큼 겹치는 꼬리를 남겨서
 * 문단 경계에서 문맥이 끊기지 않게 한다.
 */
@Component
public class TextChunker {

    private static final int TARGET_CHUNK_SIZE = 800;
    private static final int OVERLAP = 100;

    public List<String> chunk(String text) {
        String normalized = text.replaceAll("\\r\\n?", "\n").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String rawParagraph : normalized.split("\n{2,}")) {
            String paragraph = rawParagraph.trim().replaceAll("\\s+", " ");
            if (paragraph.isEmpty()) {
                continue;
            }

            if (paragraph.length() > TARGET_CHUNK_SIZE) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                chunks.addAll(splitLong(paragraph));
                continue;
            }

            if (current.length() + paragraph.length() + 1 > TARGET_CHUNK_SIZE) {
                chunks.add(current.toString());
                String tail = tailOverlap(current.toString());
                current.setLength(0);
                current.append(tail);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(paragraph);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private List<String> splitLong(String text) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + TARGET_CHUNK_SIZE, text.length());
            parts.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start = end - OVERLAP;
        }
        return parts;
    }

    private String tailOverlap(String text) {
        if (text.length() <= OVERLAP) {
            return text;
        }
        return text.substring(text.length() - OVERLAP);
    }
}
