package com.finfive.crisfin.infra.llm;

import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local-mock"})
@Slf4j
public class MockLlmProvider implements LlmProvider {

    private static final String MOCK_JSON = """
            {
              "todos": [
                {
                  "dayRange": "D+0~3",
                  "action": "진단서 발급",
                  "deadline": "퇴원 전",
                  "priority": "HIGH",
                  "reason": "보험금 청구용"
                }
              ],
              "receivable": [
                {
                  "name": "본인부담상한제 환급",
                  "estimatedMin": 500000,
                  "estimatedMax": 820000,
                  "source": "국민건강보험공단",
                  "applyUrl": "https://www.nhis.or.kr",
                  "deadline": "진료년도 다음해 8월",
                  "requiredDocs": ["진료비영수증"]
                }
              ],
              "holdable": [
                {
                  "name": "신용대출 이자 유예",
                  "deferPeriod": "최대 6개월",
                  "riskLevel": "LOW",
                  "howTo": "은행 앱에서 신청",
                  "caution": "이자는 계속 발생"
                }
              ],
              "actions": [
                {
                  "name": "실손보험 청구",
                  "requiredDocs": ["진단서", "영수증"],
                  "deadline": "3년 이내",
                  "contactInfo": "실손24 앱",
                  "priority": "HIGH"
                }
              ],
              "summary": {
                "totalReceivableMin": 1500000,
                "totalReceivableMax": 4300000,
                "urgentCount": 3,
                "thirtyDayPlan": "D+0~3 진단서 발급"
              },
              "disclaimer": "AI 분석 참고용입니다."
            }
            """;

    @Override
    public LlmResponse complete(LlmRequest request) {
        log.info("[MockLlmProvider] Returning hardcoded mock response (test profile)");
        return LlmResponse.builder()
                .content(MOCK_JSON)
                .providerName(getProviderName())
                .inputTokens(0)
                .outputTokens(0)
                .finishReason("stop")
                .build();
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }
}
