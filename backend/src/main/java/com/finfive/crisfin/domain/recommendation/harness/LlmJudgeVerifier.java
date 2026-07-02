package com.finfive.crisfin.domain.recommendation.harness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.domain.analysis.SystemPromptProvider;
import com.finfive.crisfin.infra.llm.LlmProviderRouter;
import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Second-pass LLM "judge" that ground-checks the generated advice against the allowed policy
 * set — the fuzzy check deterministic rules can't do. It reports todos/actions/holdable that
 * reference a program outside the crisis type's "적용 가능 제도" list or otherwise fabricate
 * facts.
 *
 * <p>Design safeguards, since the judge can itself hallucinate:</p>
 * <ul>
 *   <li>Runs only during detection ({@code sanitize=false}); it never deletes content — a
 *       flagged item drives the self-repair retry and, if it persists, surfaces as a warning
 *       rather than being auto-removed by a possibly-wrong judge.</li>
 *   <li>Gated behind {@code harness.judge.enabled} (default true) to trade latency for depth.</li>
 *   <li>Fails open: any error/timeout yields no flags and never blocks the analysis.</li>
 * </ul>
 */
@Component
@Slf4j
public class LlmJudgeVerifier implements OutputVerifier {

    private final LlmProviderRouter llmProviderRouter;
    private final SystemPromptProvider systemPromptProvider;
    private final ObjectMapper objectMapper;

    @Value("${harness.judge.enabled:true}")
    private boolean enabled;

    public LlmJudgeVerifier(LlmProviderRouter llmProviderRouter,
                            SystemPromptProvider systemPromptProvider,
                            ObjectMapper objectMapper) {
        this.llmProviderRouter = llmProviderRouter;
        this.systemPromptProvider = systemPromptProvider;
        this.objectMapper = objectMapper;
    }

    private static final String JUDGE_SYSTEM =
            "당신은 한국 금융 위기 지원 조언을 검증하는 검수관입니다. "
            + "오직 다음만 위반으로 보고하세요: "
            + "(1) '허용 제도 목록'에 없는 특정 지원제도/지원사업 이름을 명시적으로 언급, "
            + "(2) 존재하지 않는 기관·법령·전화번호·URL을 지어냄, "
            + "(3) 검증 불가능한 구체적 사실을 단언. "
            + "다음은 위반이 아니므로 절대 보고하지 마세요: 일반적인 절차·행동(구직등록, 서류 준비, "
            + "방문/온라인 신청, 상담 문의 등), 목록에 있는 제도, 일반적 재무 조언, 표현/어조 문제. "
            + "확신이 없으면 보고하지 마세요(과잉 보고 금지). "
            + "반드시 다음 JSON만 출력: {\"violations\":[{\"field\":\"todos[0].action\",\"issue\":\"설명\"}]}. "
            + "문제가 없으면 {\"violations\":[]}.";

    @Override
    public List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        // 판사는 정제 단계에서 실행하지 않는다(내용을 직접 수정하지 않으므로 불필요한 LLM 호출 방지).
        if (!enabled || sanitize) {
            return List.of();
        }

        Map<String, Object> subset = new LinkedHashMap<>();
        for (String key : List.of("todos", "actions", "holdable")) {
            Object v = resultMap.get(key);
            if (v != null) {
                subset.put(key, v);
            }
        }
        if (subset.isEmpty()) {
            return List.of();
        }

        try {
            String adviceJson = objectMapper.writeValueAsString(subset);
            // 정적 제도 목록 + 이번 분석에 실제 검색된 RAG 근거를 함께 제시해, judge가
            // '허용 범위'를 검색된 근거 기준으로 판단하도록 한다(근거 없는 claim 탐지).
            StringBuilder allowed = new StringBuilder("[허용 제도 목록]\n")
                    .append(systemPromptProvider.getPolicyBlock(ctx.crisisType()));
            if (ctx.evidence() != null && !ctx.evidence().isBlank()) {
                allowed.append("\n\n[검색된 공식 근거]\n").append(ctx.evidence());
            }
            String userMessage = allowed + "\n\n[검증 대상 조언]\n" + adviceJson;

            LlmResponse resp = llmProviderRouter.complete(LlmRequest.builder()
                    .systemPrompt(JUDGE_SYSTEM)
                    .userMessage(userMessage)
                    .maxTokens(512)
                    .temperature(0.0)
                    .build());

            return parseViolations(resp.getContent());
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            // fail-open: 판사 실패는 분석을 막지 않는다.
            log.warn("[LlmJudgeVerifier] judge 호출/파싱 실패 — 판정 생략: {}", e.getMessage());
            return List.of();
        }
    }

    private List<HarnessFlag> parseViolations(String content) {
        List<HarnessFlag> flags = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return flags;
        }
        // 코드펜스/프로즈 방어: 최외곽 {} 만 취한다.
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return flags;
        }
        try {
            JsonNode root = objectMapper.readTree(content.substring(start, end + 1));
            for (JsonNode v : root.path("violations")) {
                String field = v.path("field").asText("output");
                String issue = v.path("issue").asText("검증 범위 밖 제도 언급 가능성");
                flags.add(new HarnessFlag(field, "UNGROUNDED_CLAIM", Severity.HARD,
                        "검증 범위 밖 제도/사실 언급 가능성: " + issue, "FLAGGED"));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("[LlmJudgeVerifier] judge 응답 JSON 파싱 실패 — 판정 생략: {}", e.getMessage());
        }
        return flags;
    }
}
