package com.finfive.crisfin.domain.recommendation.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PolicyIndexingService#chunkText(String, int)} 순수 함수 단위 테스트 —
 * Spring 컨텍스트 없이 청킹 동작만 검증한다.
 */
class PolicyIndexingServiceChunkTest {

    @Test
    void shortTextYieldsSingleChunk() {
        String text = "[개요] 짧은 서비스 설명\n[지원내용] 월 10만원";

        List<String> chunks = PolicyIndexingService.chunkText(text, 500);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text.trim());
    }

    @Test
    void longTextSplitsIntoMultipleChunks() {
        // 각 라인이 약 100자, 20개 라인 → 약 2000자 → 500자 목표 시 여러 청크로 분할.
        String line = "x".repeat(100);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append(line).append('\n');
        }

        List<String> chunks = PolicyIndexingService.chunkText(sb.toString(), 500);

        assertThat(chunks.size()).isGreaterThan(1);
        // 각 청크는 목표 크기 근방 — 개별 라인이 목표보다 작으므로 크게 초과하지 않는다.
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(600));
    }

    @Test
    void blankOrNullTextYieldsEmptyList() {
        assertThat(PolicyIndexingService.chunkText(null, 500)).isEmpty();
        assertThat(PolicyIndexingService.chunkText("   ", 500)).isEmpty();
    }

    @Test
    void singleLineLongerThanTargetBecomesOwnChunk() {
        String longLine = "y".repeat(1200);

        List<String> chunks = PolicyIndexingService.chunkText(longLine, 500);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(1200);
    }
}
