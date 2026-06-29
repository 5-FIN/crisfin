package com.finfive.crisfin.domain.recommendation.rag;

import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.infra.embedding.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PolicyRetrievalService} — pure Mockito, no Spring context or DB
 * (the native pgvector SQL is therefore never executed under test, per the test policy).
 */
@ExtendWith(MockitoExtension.class)
class PolicyRetrievalServiceTest {

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private PolicyEmbeddingRepository policyEmbeddingRepository;

    @InjectMocks
    private PolicyRetrievalService policyRetrievalService;

    @Test
    void returnsEmptyAndSkipsRepository_whenEmbeddingDisabled() {
        when(embeddingProvider.isEnabled()).thenReturn(false);

        List<RetrievedPolicy> result =
                policyRetrievalService.retrieve(CrisisType.HOSPITALIZATION, "6주 입원했습니다", 5);

        assertThat(result).isEmpty();
        verify(embeddingProvider, never()).embed(anyString());
        verifyNoInteractions(policyEmbeddingRepository);
    }

    @Test
    void returnsSearchResults_whenEnabled() {
        float[] queryVector = {0.1f, 0.2f, 0.3f};
        RetrievedPolicy hit =
                new RetrievedPolicy("WELFARE", "12", "긴급복지 생계지원 ...", 0.12);

        when(embeddingProvider.isEnabled()).thenReturn(true);
        when(embeddingProvider.embed("6주 입원했습니다")).thenReturn(queryVector);
        when(policyEmbeddingRepository.searchNearest(anyString(), eq(CrisisType.HOSPITALIZATION), eq(5)))
                .thenReturn(List.of(hit));

        List<RetrievedPolicy> result =
                policyRetrievalService.retrieve(CrisisType.HOSPITALIZATION, "6주 입원했습니다", 5);

        assertThat(result).containsExactly(hit);
        verify(embeddingProvider).embed("6주 입원했습니다");
        verify(policyEmbeddingRepository).searchNearest(anyString(), eq(CrisisType.HOSPITALIZATION), eq(5));
    }

    @Test
    void returnsEmpty_whenRepositoryThrows() {
        when(embeddingProvider.isEnabled()).thenReturn(true);
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(policyEmbeddingRepository.searchNearest(anyString(), any(), anyInt()))
                .thenThrow(new RuntimeException("db unavailable"));

        List<RetrievedPolicy> result =
                policyRetrievalService.retrieve(CrisisType.ACCIDENT, "교통사고", 5);

        assertThat(result).isEmpty();
    }
}
