package com.finfive.crisfin.infra.llm;

import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Deterministic mock LLM used under the {@code test} / {@code local-mock} profiles.
 *
 * <p>The real analysis prompt embeds the crisis type as the first line of the user message
 * ({@code "위기 유형: <ENUM> (<label>)"}, see {@code AnalysisService#buildUserMessage}). This
 * mock parses that marker and branches so every crisis situation returns its own distinct
 * strategy mock (todos / holdable / actions / summary) instead of one hospitalization blob.</p>
 *
 * <p>Amounts still come from the deterministic rule engine — the {@code receivable} block here
 * is a schema-completing placeholder that {@code ResultAssembler} overwrites. All narrative
 * text is kept amount-free and every {@code contactInfo} uses an official allowlisted hotline
 * so the hallucination harness passes without sanitizing the demo output.</p>
 */
@Component
@Profile({"test", "local-mock"})
@Slf4j
public class MockLlmProvider implements LlmProvider {

    /** Per-crisis mock strategy JSON, keyed by crisis type. */
    private static final Map<CrisisType, String> MOCK_BY_CRISIS = Map.of(
            CrisisType.HOSPITALIZATION, HOSPITALIZATION_JSON(),
            CrisisType.ACCIDENT, ACCIDENT_JSON(),
            CrisisType.UNEMPLOYMENT, UNEMPLOYMENT_JSON(),
            CrisisType.CAREGIVING, CAREGIVING_JSON(),
            CrisisType.BEREAVEMENT, BEREAVEMENT_JSON()
    );

    @Override
    public LlmResponse complete(LlmRequest request) {
        CrisisType crisisType = parseCrisisType(request.getUserMessage());
        log.info("[MockLlmProvider] Returning per-crisis mock response (crisisType={})", crisisType);
        return LlmResponse.builder()
                .content(MOCK_BY_CRISIS.get(crisisType))
                .providerName(getProviderName())
                .inputTokens(0)
                .outputTokens(0)
                .finishReason("stop")
                .build();
    }

    /**
     * Extracts the crisis type from the analysis user message. Falls back to
     * {@link CrisisType#HOSPITALIZATION} when the marker is absent (e.g. an LLM-judge
     * sub-prompt), preserving the previous single-blob behaviour for non-analysis calls.
     */
    private CrisisType parseCrisisType(String userMessage) {
        if (userMessage != null) {
            for (CrisisType type : CrisisType.values()) {
                if (userMessage.contains("위기 유형: " + type.name())) {
                    return type;
                }
            }
        }
        return CrisisType.HOSPITALIZATION;
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    // ------------------------------------------------------------------ //
    //  Per-crisis mock payloads (amounts live in the rule engine, not here)
    // ------------------------------------------------------------------ //

    private static String HOSPITALIZATION_JSON() {
        return """
                {
                  "todos": [
                    {"dayRange":"D+0~3","action":"진단서·입퇴원확인서 발급받기","deadline":"퇴원 전","priority":"HIGH","reason":"실손보험·본인부담상한제 청구에 필요한 기본 서류입니다."},
                    {"dayRange":"D+3~7","action":"국민건강보험공단에 본인부담상한제 사전급여 대상 확인","deadline":"가능한 빨리","priority":"HIGH","reason":"연간 의료비가 상한을 넘으면 초과분을 돌려받을 수 있습니다."},
                    {"dayRange":"D+7~30","action":"가입한 실손·질병보험 보장 항목 정리","deadline":"청구 전","priority":"MED","reason":"중복 보장과 청구 기한을 놓치지 않기 위함입니다."}
                  ],
                  "receivable": [
                    {"name":"건강보험 본인부담상한제 환급","estimatedMin":0,"estimatedMax":0,"source":"국민건강보험공단","applyUrl":"https://www.nhis.or.kr","deadline":"진료년도 다음해 8월","requiredDocs":["진료비 영수증","본인 명의 계좌"]}
                  ],
                  "holdable": [
                    {"name":"카드 결제대금","deferPeriod":"최대 3개월","riskLevel":"MED","howTo":"카드사 앱에서 결제일 연기 또는 결제 유예를 신청하세요.","caution":"유예 기간에도 수수료가 붙을 수 있어 조건을 확인하세요."},
                    {"name":"통신·공과금","deferPeriod":"1~2개월","riskLevel":"LOW","howTo":"통신사·해당 기관에 납부유예를 신청하세요.","caution":"연체 등록 전에 신청해야 신용에 영향이 없습니다."}
                  ],
                  "actions": [
                    {"name":"실손보험 청구","requiredDocs":["진단서","진료비 영수증"],"deadline":"3년 이내","contactInfo":"가입 보험사 앱 또는 콜센터","priority":"HIGH"},
                    {"name":"본인부담상한제 환급 신청","requiredDocs":["진료비 영수증","본인 명의 계좌"],"deadline":"진료년도 다음해 8월","contactInfo":"국민건강보험공단 1577-1000","priority":"HIGH"}
                  ],
                  "summary": {
                    "totalReceivableMin":0,
                    "totalReceivableMax":0,
                    "urgentCount":3,
                    "thirtyDayPlan":"D+0~3 진단서 발급 → 실손보험 청구 → 본인부담상한제 환급 신청 순으로 진행하세요."
                  },
                  "disclaimer": "AI 분석 참고용입니다. 정확한 자격·금액·기한은 관할 기관에 확인하세요."
                }
                """;
    }

    private static String ACCIDENT_JSON() {
        return """
                {
                  "todos": [
                    {"dayRange":"D+0~3","action":"사고 경위서·목격자 정보 정리 및 병원 진단서 확보","deadline":"가능한 빨리","priority":"HIGH","reason":"산재 신청과 배상 절차의 기본 증빙입니다."},
                    {"dayRange":"D+3~14","action":"근로복지공단에 산재(요양·휴업급여) 신청 가능 여부 확인","deadline":"요양 개시 후 빠를수록 유리","priority":"HIGH","reason":"업무상 재해로 인정되면 치료비와 휴업급여를 받을 수 있습니다."},
                    {"dayRange":"D+7~30","action":"자동차·운전자보험 및 배상책임 보장 확인","deadline":"청구 기한 내","priority":"MED","reason":"교통사고라면 자동차보험 합의와 병행해야 합니다."}
                  ],
                  "receivable": [
                    {"name":"산업재해보상보험 급여","estimatedMin":0,"estimatedMax":0,"source":"근로복지공단","applyUrl":"https://www.comwel.or.kr","deadline":"요양 개시 후 3년","requiredDocs":["산재 요양급여 신청서","재해 경위서"]}
                  ],
                  "holdable": [
                    {"name":"대출 원리금","deferPeriod":"최대 6개월","riskLevel":"MED","howTo":"거래 은행에 상환유예·채무조정을 문의하세요.","caution":"유예해도 이자는 계속 발생하니 조건을 확인하세요."},
                    {"name":"카드 결제대금","deferPeriod":"1~3개월","riskLevel":"LOW","howTo":"카드사에 결제 유예를 신청하세요.","caution":"연체 등록 전에 신청하세요."}
                  ],
                  "actions": [
                    {"name":"산재보험 급여 신청","requiredDocs":["산재 요양급여 신청서","재해 경위서"],"deadline":"요양 개시 후 3년","contactInfo":"근로복지공단 1588-0075","priority":"HIGH"},
                    {"name":"긴급복지 지원 상담","requiredDocs":["신분증","위기상황 증빙"],"deadline":"가능한 빨리","contactInfo":"보건복지상담센터 129","priority":"MED"}
                  ],
                  "summary": {
                    "totalReceivableMin":0,
                    "totalReceivableMax":0,
                    "urgentCount":3,
                    "thirtyDayPlan":"사고 증빙 확보 → 근로복지공단 산재 신청 → 보험·배상 절차를 순차 진행하세요."
                  },
                  "disclaimer": "AI 분석 참고용입니다. 정확한 자격·금액·기한은 관할 기관에 확인하세요."
                }
                """;
    }

    private static String UNEMPLOYMENT_JSON() {
        return """
                {
                  "todos": [
                    {"dayRange":"D+0~7","action":"이직확인서·고용보험 피보험자격 이력 발급","deadline":"퇴사 직후","priority":"HIGH","reason":"실업급여 신청의 필수 서류입니다."},
                    {"dayRange":"D+3~14","action":"워크넷 구직 등록 후 고용센터에 실업급여 수급자격 신청","deadline":"퇴직 다음날부터 12개월 이내","priority":"HIGH","reason":"신청이 늦으면 받을 수 있는 일수가 줄어듭니다."},
                    {"dayRange":"D+7~30","action":"국민연금 납부예외·건강보험료 조정 신청","deadline":"소득단절 확인 후","priority":"MED","reason":"소득이 없는 기간의 보험료 부담을 줄입니다."}
                  ],
                  "receivable": [
                    {"name":"실업급여(구직급여)","estimatedMin":0,"estimatedMax":0,"source":"고용노동부","applyUrl":"https://www.ei.go.kr","deadline":"퇴직 다음 날부터 12개월 이내","requiredDocs":["이직확인서","고용보험 피보험자격 이력"]}
                  ],
                  "holdable": [
                    {"name":"대출 원리금","deferPeriod":"최대 6~12개월","riskLevel":"MED","howTo":"은행에 실직 사유 채무조정·상환유예를 신청하세요.","caution":"유예 기간의 이자 조건을 확인하세요."},
                    {"name":"국민연금 보험료","deferPeriod":"소득단절 기간","riskLevel":"LOW","howTo":"국민연금공단에 납부예외를 신청하세요.","caution":"가입기간 인정 여부를 함께 확인하세요."}
                  ],
                  "actions": [
                    {"name":"실업급여 수급자격 신청","requiredDocs":["이직확인서","신분증"],"deadline":"퇴직 다음 날부터 12개월 이내","contactInfo":"고용노동부 1350","priority":"HIGH"},
                    {"name":"국민연금 납부예외 신청","requiredDocs":["신분증"],"deadline":"소득단절 후","contactInfo":"국민연금공단 1355","priority":"MED"}
                  ],
                  "summary": {
                    "totalReceivableMin":0,
                    "totalReceivableMax":0,
                    "urgentCount":3,
                    "thirtyDayPlan":"이직확인서 발급 → 워크넷 구직등록 → 실업급여 신청을 2주 안에 마치세요."
                  },
                  "disclaimer": "AI 분석 참고용입니다. 정확한 자격·금액·기한은 관할 기관에 확인하세요."
                }
                """;
    }

    private static String CAREGIVING_JSON() {
        return """
                {
                  "todos": [
                    {"dayRange":"D+0~7","action":"국민건강보험공단에 재난적의료비 지원 대상 확인","deadline":"가능한 빨리","priority":"HIGH","reason":"과다한 본인부담 의료비의 일부를 지원받을 수 있습니다."},
                    {"dayRange":"D+3~14","action":"가족돌봄휴가·가족돌봄휴직 신청 가능 여부 확인","deadline":"돌봄 시작 후","priority":"HIGH","reason":"소득단절을 줄이며 간병을 병행할 수 있습니다."},
                    {"dayRange":"D+14~30","action":"간병비·의료비 지출 내역 정리 및 지원제도 매칭","deadline":"청구 기한 내","priority":"MED","reason":"중복 지원과 청구 기한을 놓치지 않기 위함입니다."}
                  ],
                  "receivable": [
                    {"name":"재난적의료비 지원","estimatedMin":0,"estimatedMax":0,"source":"국민건강보험공단","applyUrl":"https://www.nhis.or.kr","deadline":"진료 종료 후 180일 이내","requiredDocs":["진단서","진료비 영수증","소득·재산 증빙"]}
                  ],
                  "holdable": [
                    {"name":"대출 원리금","deferPeriod":"최대 6개월","riskLevel":"MED","howTo":"거래 은행에 상환유예를 문의하세요.","caution":"유예 기간의 이자 조건을 확인하세요."},
                    {"name":"공과금·보험료","deferPeriod":"기관별 상이","riskLevel":"LOW","howTo":"해당 기관에 분납·유예를 신청하세요.","caution":"연체 처리되지 않도록 확인하세요."}
                  ],
                  "actions": [
                    {"name":"재난적의료비 지원 신청","requiredDocs":["진단서","진료비 영수증","소득·재산 증빙"],"deadline":"진료 종료 후 180일 이내","contactInfo":"국민건강보험공단 1577-1000","priority":"HIGH"},
                    {"name":"가족돌봄 지원 상담","requiredDocs":["가족관계증명서"],"deadline":"돌봄 기간 내","contactInfo":"고용노동부 1350","priority":"MED"}
                  ],
                  "summary": {
                    "totalReceivableMin":0,
                    "totalReceivableMax":0,
                    "urgentCount":2,
                    "thirtyDayPlan":"재난적의료비 지원 확인 → 가족돌봄휴직 신청 → 의료비 지출 정리 순으로 진행하세요."
                  },
                  "disclaimer": "AI 분석 참고용입니다. 정확한 자격·금액·기한은 관할 기관에 확인하세요."
                }
                """;
    }

    private static String BEREAVEMENT_JSON() {
        return """
                {
                  "todos": [
                    {"dayRange":"D+0~7","action":"사망신고 및 정부24 안심상속 원스톱 서비스 신청","deadline":"사망일이 속한 달 말일부터 6개월 이내","priority":"HIGH","reason":"사망자의 금융재산·보험·연금을 한 번에 조회합니다."},
                    {"dayRange":"D+7~14","action":"국민연금 유족연금·사망일시금 청구 가능 여부 확인","deadline":"가능한 빨리","priority":"HIGH","reason":"유족 생계 보전을 위한 핵심 급여입니다."},
                    {"dayRange":"D+14~30","action":"상속재산 확인 후 상속 승인·한정승인·포기 판단","deadline":"상속 개시 후 3개월 이내","priority":"MED","reason":"빚이 많을 경우 한정승인·포기로 부담을 막습니다."}
                  ],
                  "receivable": [
                    {"name":"안심상속 원스톱 서비스","estimatedMin":0,"estimatedMax":0,"source":"행정안전부","applyUrl":"https://www.gov.kr","deadline":"사망일이 속한 달의 말일부터 6개월 이내","requiredDocs":["사망진단서","상속인 신분증"]}
                  ],
                  "holdable": [
                    {"name":"고인 명의 대출·카드","deferPeriod":"협의 필요","riskLevel":"MED","howTo":"금융기관에 사망 사실을 알리고 상속 절차를 협의하세요.","caution":"연체·추심 처리 전에 통지해야 합니다."},
                    {"name":"공과금·구독료","deferPeriod":"정리 기간","riskLevel":"LOW","howTo":"자동이체를 해지하고 명의를 정리하세요.","caution":"불필요한 지출이 계속되지 않도록 합니다."}
                  ],
                  "actions": [
                    {"name":"안심상속 원스톱 서비스 신청","requiredDocs":["사망진단서","상속인 신분증"],"deadline":"사망일이 속한 달의 말일부터 6개월 이내","contactInfo":"정부민원안내 110","priority":"HIGH"},
                    {"name":"국민연금 유족연금 청구","requiredDocs":["사망진단서","가족관계증명서"],"deadline":"가능한 빨리","contactInfo":"국민연금공단 1355","priority":"HIGH"}
                  ],
                  "summary": {
                    "totalReceivableMin":0,
                    "totalReceivableMax":0,
                    "urgentCount":3,
                    "thirtyDayPlan":"사망신고·안심상속 조회 → 유족연금 청구 → 상속 방식 결정 순으로 진행하세요."
                  },
                  "disclaimer": "AI 분석 참고용입니다. 정확한 자격·금액·기한은 관할 기관에 확인하세요."
                }
                """;
    }
}
