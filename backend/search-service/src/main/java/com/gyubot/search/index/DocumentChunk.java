package com.gyubot.search.index;

import java.time.Instant;
import java.util.List;

/*
 * Elasticsearch에 그대로 색인되는 문서. 청크 텍스트(BM25 검색용)와 임베딩 벡터(kNN 검색용)를
 * 같은 문서에 함께 담아, 별도 Vector DB 없이 이 인덱스 하나로 하이브리드 검색을 한다.
 *
 * embedding은 float[]가 아니라 List<Float>로 둔다 — co.elastic.clients의 JacksonJsonpMapper가
 * POJO를 색인할 때 원시 float[] 필드는 조용히 건너뛰고 직렬화하지 않는 걸 실제로 확인했다
 * (에러 없이 그냥 _source에서 빠짐). List<Float>로 바꾸니 정상 직렬화됨.
 */
public record DocumentChunk(
        Long documentId,
        Long companyId,
        int chunkIndex,
        String text,
        List<Float> embedding,
        String originalFilename,
        String contentType,
        Instant createdAt
) {
    public String id() {
        return documentId + "-" + chunkIndex;
    }
}
