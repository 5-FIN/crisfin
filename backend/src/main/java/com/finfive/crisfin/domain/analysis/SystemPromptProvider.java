package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.crisis.CrisisType;
import org.springframework.stereotype.Component;

/**
 * Provides the system prompt for the LLM financial crisis analysis call.
 *
 * <p>Follows the CrisisFin LLM strategy (컨텍스트 스터핑 기반 경량 RAG): instead of injecting
 * every welfare programme for every request, only the programmes relevant to the current
 * {@link CrisisType} are stuffed into the prompt. This keeps the model grounded in a narrow,
 * on-topic policy set (전략1 컨텍스트 최적화 — 위기 유형별 관련 제도만 주입) and reduces
 * hallucination surface. The role header and the strict JSON output contract (+ 금액 표현 원칙)
 * are shared across all crisis types.</p>
 */
@Component
public class SystemPromptProvider {

    /** 공통 역할 지시 — 위기별 제도 블록 앞에 붙는다. */
    private static final String ROLE_HEADER =
            "당신은 한국 금융 위기 지원 전문가입니다. 아래 '적용 가능 제도'는 이번 위기 유형에 " +
            "해당하는 제도만 담고 있습니다. 반드시 이 범위 안에서만 근거를 들어 답하고, " +
            "목록에 없는 제도를 임의로 만들어내지 마세요.\n";

    /**
     * 공통 출력 계약 — JSON 스키마 강제 + 금액 표현 원칙(환각 방지).
     * 위기별 제도 블록 뒤에 붙는다.
     */
    private static final String OUTPUT_CONTRACT =
            "\n[금액 표현 원칙]\n" +
            "- 금액은 반드시 구간(estimatedMin~estimatedMax)으로만 제시하고 확정 금액을 단언하지 마세요.\n" +
            "- '반드시', '확실히', '무조건' 등 단정 표현을 쓰지 마세요.\n" +
            "- 모든 receivable 항목에는 근거가 되는 source(제도/기관)를 반드시 채우세요.\n" +
            "- 자격·금액이 불확실하면 금액을 지어내지 말고 추가 확인이 필요하다고 서술하세요.\n" +
            "- 특정 금융상품(대출/보험/카드/적금 등)의 가입·신청을 권유하지 마세요. " +
            "대신 상환 유예 가능 여부, 보장 범위, 공공지원·복지제도 확인 등 '정보 확인' 관점으로 안내하세요.\n" +
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

    // ── 위기 유형별 적용 가능 제도(컨텍스트 스터핑) — 규칙 엔진이 산정하는 제도와 정렬 ── //

    private static final String POLICIES_HOSPITALIZATION =
            "[적용 가능 제도]\n" +
            "[본인부담상한제]: 연간 본인부담 의료비가 소득분위별 상한을 초과하면 차액 환급. 직접 신청 필요. 국민건강보험공단.\n" +
            "[실손보험 청구]: 가입한 실손의료보험 청구. 실손24 앱으로 간편 청구. 청구 기한 3년.\n" +
            "[긴급복지 의료지원]: 중위소득 75% 이하·재산 기준 충족 시 의료비 지원(최대 300만원).\n";

    private static final String POLICIES_ACCIDENT =
            "[적용 가능 제도]\n" +
            "[산재보험]: 업무상 재해로 인정되면 요양급여·휴업급여(평균임금 70%) 등 지급. 근로복지공단. 재해 경위 확인 필요.\n" +
            "[실손보험 청구]: 상해 치료비 실손 청구. 실손24 앱. 청구 기한 3년.\n" +
            "[긴급복지 생계지원]: 주소득자의 사고로 소득이 끊긴 경우 생계지원 대상 가능.\n";

    private static final String POLICIES_UNEMPLOYMENT =
            "[적용 가능 제도]\n" +
            "[실업급여(구직급여)]: 고용보험 가입 180일 이상 + 비자발적 이직 시. 이전 평균임금의 약 60%, 소정급여일수 120~270일.\n" +
            "[긴급복지 생계지원]: 실직으로 소득이 끊기고 중위소득 75% 이하·재산 기준 충족 시 생계지원(최대 3개월).\n";

    private static final String POLICIES_CAREGIVING =
            "[적용 가능 제도]\n" +
            "[노인장기요양보험]: 장기요양 1~5등급 판정 시 재가/시설 급여. 등급별 월 한도 내 본인부담 15% 제외 지원.\n" +
            "[긴급복지 생계지원]: 간병으로 소득활동이 어려워 소득·재산 기준 충족 시 생계지원 대상 가능.\n";

    private static final String POLICIES_BEREAVEMENT =
            "[적용 가능 제도]\n" +
            "[안심상속 원스톱 서비스]: 사망자의 금융재산·보험·연금·세금을 일괄 조회. 사망일이 속한 달 말일부터 6개월 이내 신청.\n" +
            "[긴급복지 생계지원]: 주소득자 사망으로 생계가 곤란하고 소득·재산 기준 충족 시 생계지원 대상 가능.\n";

    /**
     * Returns the full system prompt for the given crisis type: 공통 역할 + 위기별 제도 + 출력 계약.
     *
     * @param crisisType the crisis category being analysed
     * @return system prompt string tailored to {@code crisisType}
     */
    public String getSystemPrompt(CrisisType crisisType) {
        return ROLE_HEADER + policiesFor(crisisType) + OUTPUT_CONTRACT;
    }

    /**
     * Returns the crisis-type "적용 가능 제도" block — the authoritative set of programs the
     * analysis may reference. Exposed for the harness LLM-judge to ground-check the output.
     *
     * @param crisisType the crisis category
     * @return the policy block text for the type
     */
    public String getPolicyBlock(CrisisType crisisType) {
        return policiesFor(crisisType);
    }

    /** 위기 유형별 적용 가능 제도 블록. */
    private String policiesFor(CrisisType crisisType) {
        return switch (crisisType) {
            case HOSPITALIZATION -> POLICIES_HOSPITALIZATION;
            case ACCIDENT -> POLICIES_ACCIDENT;
            case UNEMPLOYMENT -> POLICIES_UNEMPLOYMENT;
            case CAREGIVING -> POLICIES_CAREGIVING;
            case BEREAVEMENT -> POLICIES_BEREAVEMENT;
        };
    }
}
