package com.gyubot.chat.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * 형태소 분석기 없이 최근 질문 텍스트에서 자주 나오는 단어를 순수 빈도 기반으로 뽑는다.
 * 정확한 한국어 형태소 분석은 아니지만(조사가 붙은 채로 카운트될 수 있음), 관리자 대시보드의
 * "요즘 뭘 자주 물어보나" 감을 잡는 용도로는 충분하다 — 별도 NLP 의존성을 추가하지 않기 위한
 * 의도적인 단순화.
 */
@Component
public class KeywordExtractor {

    private static final Set<String> STOPWORDS = Set.of(
            "있나요", "있어요", "있습니까", "있어", "무엇인가요", "어떻게", "어떤가요", "궁금합니다",
            "알려주세요", "알려줘", "인가요", "될까요", "됩니까", "하나요", "하는지", "언제까지",
            "얼마나", "대해서", "대한", "관련", "그리고", "그러면", "때문에");

    public List<KeywordCount> topKeywords(List<String> texts, int limit) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String text : texts) {
            for (String token : text.split("[\\s,./?!()\\[\\]{}\"'…·]+")) {
                String word = token.trim();
                if (word.length() < 2 || STOPWORDS.contains(word)) {
                    continue;
                }
                counts.merge(word, 1, Integer::sum);
            }
        }
        List<KeywordCount> sorted = new ArrayList<>();
        counts.forEach((word, count) -> sorted.add(new KeywordCount(word, count)));
        sorted.sort(Comparator.comparingInt(KeywordCount::count).reversed());
        return sorted.size() > limit ? sorted.subList(0, limit) : sorted;
    }

    public record KeywordCount(String keyword, int count) {
    }
}
