package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.crisis.CrisisType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SystemPromptProvider} — 프롬프트가 JSON 스키마/전문가 지침을 포함하는지.
 */
class SystemPromptProviderTest {

    private final SystemPromptProvider provider = new SystemPromptProvider();

    @Test
    void getSystemPrompt_containsExpertRoleAndJsonSchema() {
        String prompt = provider.getSystemPrompt(CrisisType.UNEMPLOYMENT);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("전문가");
        // 필수 JSON 스키마 키들이 들어있어야 함
        assertThat(prompt).contains("todos", "receivable", "holdable", "actions", "summary", "disclaimer");
    }

    @Test
    void getSystemPrompt_containsAmountExpressionRules() {
        // 금액 표현 원칙(환각 방지)이 모든 위기유형 프롬프트에 공통으로 포함되어야 함
        for (CrisisType type : CrisisType.values()) {
            String prompt = provider.getSystemPrompt(type);
            assertThat(prompt).contains("구간", "source", "disclaimer");
        }
    }

    @Test
    void getSystemPrompt_injectsOnlyCrisisRelevantPolicies() {
        // 위기 유형별로 관련 제도만 주입한다(전략1 컨텍스트 최적화).
        String unemployment = provider.getSystemPrompt(CrisisType.UNEMPLOYMENT);
        assertThat(unemployment).contains("실업급여");
        assertThat(unemployment).doesNotContain("산재보험", "장기요양", "안심상속");

        String hospitalization = provider.getSystemPrompt(CrisisType.HOSPITALIZATION);
        assertThat(hospitalization).contains("본인부담상한제");
        assertThat(hospitalization).doesNotContain("실업급여", "안심상속");
    }

    @Test
    void getSystemPrompt_differsAcrossCrisisTypes() {
        // 더 이상 모든 위기유형이 동일 프롬프트를 공유하지 않는다.
        String a = provider.getSystemPrompt(CrisisType.HOSPITALIZATION);
        String b = provider.getSystemPrompt(CrisisType.BEREAVEMENT);

        assertThat(a).isNotEqualTo(b);
    }
}
