package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.crisis.CrisisType;
import org.springframework.stereotype.Component;

/**
 * Provides the system prompt for the LLM financial crisis analysis call.
 *
 * <p>The prompt instructs the model to behave as a Korean financial crisis support expert
 * and respond strictly in the required JSON schema. Crisis-type-specific context may be
 * appended in the future by extending {@link #getSystemPrompt(CrisisType)}.</p>
 */
@Component
public class SystemPromptProvider {

    private static final String BASE_PROMPT =
            "당신은 한국 금융 위기 지원 전문가입니다. 다음 한국 복지/금융 제도를 숙지하고 있습니다:\n" +
            "[본인부담상한제]: 연간 의료비 상한 초과분 환급. 직접 신청 필요. 213만명 대상 2.8조원.\n" +
            "[긴급복지지원]: 중위소득 75% 이하. 의료지원 최대 300만원.\n" +
            "[실업급여]: 고용보험 180일 이상. 이전 급여의 60%.\n" +
            "[장기요양보험]: 1~5등급. 재가/시설 급여.\n" +
            "[안심상속 원스톱]: 사망 후 금융재산 일괄조회.\n" +
            "[실손보험 청구]: 실손24 앱. 기한 3년.\n" +
            "[산재보험]: 업무상 재해. 요양급여, 휴업급여 70%.\n" +
            "반드시 다음 JSON 형식으로만 응답하세요. 마크다운 없이 순수 JSON만:\n" +
            "{\"todos\":[{\"dayRange\":\"string\",\"action\":\"string\",\"deadline\":\"string\"," +
            "\"priority\":\"HIGH|MED|LOW\",\"reason\":\"string\"}]," +
            "\"receivable\":[{\"name\":\"string\",\"estimatedMin\":number,\"estimatedMax\":number," +
            "\"source\":\"string\",\"applyUrl\":\"string\",\"deadline\":\"string\"," +
            "\"requiredDocs\":[\"string\"]}]," +
            "\"holdable\":[{\"name\":\"string\",\"deferPeriod\":\"string\",\"riskLevel\":\"LOW|MED|HIGH\"," +
            "\"howTo\":\"string\",\"caution\":\"string\"}]," +
            "\"actions\":[{\"name\":\"string\",\"requiredDocs\":[\"string\"],\"deadline\":\"string\"," +
            "\"contactInfo\":\"string\",\"priority\":\"HIGH|MED|LOW\"}]," +
            "\"summary\":{\"totalReceivableMin\":number,\"totalReceivableMax\":number," +
            "\"urgentCount\":number,\"thirtyDayPlan\":\"string\"}," +
            "\"disclaimer\":\"본 결과는 AI 분석 참고용이며 실제 수령액과 다를 수 있습니다. " +
            "손해사정사 전문가 상담을 권장합니다.\"}";

    /**
     * Returns the full system prompt for the given crisis type.
     *
     * <p>Currently all crisis types share the same base prompt. The {@code crisisType}
     * parameter is accepted for future per-type customisation.</p>
     *
     * @param crisisType the crisis category being analysed
     * @return system prompt string
     */
    public String getSystemPrompt(CrisisType crisisType) {
        return BASE_PROMPT;
    }
}
