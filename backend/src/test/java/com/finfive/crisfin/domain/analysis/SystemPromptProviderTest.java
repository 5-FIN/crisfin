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
    void getSystemPrompt_isStableAcrossCrisisTypes() {
        // 현재는 모든 위기유형이 동일한 베이스 프롬프트를 공유한다.
        String a = provider.getSystemPrompt(CrisisType.HOSPITALIZATION);
        String b = provider.getSystemPrompt(CrisisType.BEREAVEMENT);

        assertThat(a).isEqualTo(b);
    }
}
