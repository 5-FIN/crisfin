package com.finfive.crisfin.domain.welfare;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WelfareCrisisTagger} — 키워드 → 위기유형 태그 매핑.
 */
class WelfareCrisisTaggerTest {

    private final WelfareCrisisTagger tagger = new WelfareCrisisTagger();

    @Test
    void tag_unemploymentKeyword_returnsUnemployment() {
        // 서비스명·요약에 '실업/구직' 키워드 → UNEMPLOYMENT
        assertThat(tagger.tag("실업급여 지원", "구직활동 지원금", null, null))
                .containsExactly("UNEMPLOYMENT");
    }

    @Test
    void tag_medicalKeyword_returnsHospitalization() {
        assertThat(tagger.tag("의료비 지원", "입원 치료비 경감", "건강", null))
                .containsExactly("HOSPITALIZATION");
    }

    @Test
    void tag_caregivingKeyword_returnsCaregiving() {
        assertThat(tagger.tag("장애인 돌봄 서비스", "요양 지원", null, null))
                .containsExactly("CAREGIVING");
    }

    @Test
    void tag_bereavementKeyword_returnsBereavement() {
        assertThat(tagger.tag("유족 급여", "장제비 지원", null, null))
                .containsExactly("BEREAVEMENT");
    }

    @Test
    void tag_accidentKeyword_returnsAccident() {
        assertThat(tagger.tag("재해 피해 복구 지원", null, "재난", null))
                .containsExactly("ACCIDENT");
    }

    @Test
    void tag_multipleKeywords_returnsAllMatchesInInsertionOrder() {
        // 장애인 의료비 → CAREGIVING + HOSPITALIZATION, 삽입 순서(HOSPITALIZATION 먼저)로 정렬
        assertThat(tagger.tag("저소득 장애인 의료비 지원", null, null, null))
                .containsExactly("HOSPITALIZATION", "CAREGIVING");
    }

    @Test
    void tag_noKeyword_returnsEmpty() {
        assertThat(tagger.tag("문화누리카드 발급", "여가 활동 바우처", null, null))
                .isEmpty();
    }

    @Test
    void tag_allNulls_returnsEmpty() {
        assertThat(tagger.tag(null, null, null, null)).isEmpty();
    }

    @Test
    void tag_matchesFromLifeCycleAndThemeFields() {
        // 관심주제/생애주기 텍스트에서도 키워드를 잡아야 함
        assertThat(tagger.tag("바우처", null, "고용", "청년"))
                .containsExactly("UNEMPLOYMENT");
    }
}
