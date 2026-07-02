package com.finfive.crisfin.domain.recommendation.harness;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hallucination-guard harness for money-critical LLM output.
 *
 * <p>Runs every {@link OutputVerifier} over the assembled result. The recommended flow
 * (owned by the caller so the retry can re-invoke the LLM):</p>
 * <ol>
 *   <li>{@link #detect(Map, HarnessContext)} after the first generation.</li>
 *   <li>If {@link #hasHard(List)}, re-call the LLM with {@link #buildRepairFeedback(List)}
 *       appended, then {@code detect} again (one retry).</li>
 *   <li>{@link #sanitize(Map, HarnessContext)} to redact any remaining HARD violations
 *       and obtain the final flag set to persist/surface.</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisHarness {

    private final List<OutputVerifier> verifiers;

    /** Detects violations without mutating the result. */
    public List<HarnessFlag> detect(Map<String, Object> resultMap, HarnessContext ctx) {
        return run(resultMap, ctx, false);
    }

    /** Redacts HARD violations in place and returns the resulting (STRIPPED/FLAGGED) flags. */
    public List<HarnessFlag> sanitize(Map<String, Object> resultMap, HarnessContext ctx) {
        List<HarnessFlag> flags = run(resultMap, ctx, true);
        long stripped = flags.stream().filter(f -> "STRIPPED".equals(f.action())).count();
        if (stripped > 0) {
            log.warn("[AnalysisHarness] {}건의 HARD 위반을 정제(제거)했습니다. crisisType={}",
                    stripped, ctx.crisisType());
        }
        return flags;
    }

    private List<HarnessFlag> run(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();
        for (OutputVerifier v : verifiers) {
            try {
                flags.addAll(v.inspect(resultMap, ctx, sanitize));
            } catch (RuntimeException e) {
                // 검증기 하나가 실패해도 분석 전체를 막지 않는다(안전 우선, 로깅 후 계속).
                log.warn("[AnalysisHarness] verifier {} 실패: {}", v.getClass().getSimpleName(), e.getMessage());
            }
        }
        return flags;
    }

    /** True if any flag is {@link Severity#HARD}. */
    public static boolean hasHard(List<HarnessFlag> flags) {
        return flags.stream().anyMatch(f -> f.severity() == Severity.HARD);
    }

    /**
     * Merges the final surfaced flag set. {@link #sanitize(Map, HarnessContext)} produces the
     * mutating verifiers' results (contact/amount marked {@code STRIPPED}, plus SOFT flags)
     * but skips detect-only verifiers such as the LLM judge. We therefore start from the last
     * {@code detectFlags} (which include the judge's semantic findings) and upgrade any flag
     * that was actually stripped to {@code STRIPPED}, so nothing detected is lost from the
     * response while stripped fields are reported accurately.
     *
     * @param detectFlags    flags from the final detect pass (includes judge/consistency)
     * @param sanitizedFlags flags from the sanitize pass (subset that could be redacted)
     * @return the reconciled flag list to persist/surface
     */
    public List<HarnessFlag> finalizeFlags(List<HarnessFlag> detectFlags, List<HarnessFlag> sanitizedFlags) {
        java.util.Set<String> stripped = sanitizedFlags.stream()
                .filter(f -> "STRIPPED".equals(f.action()))
                .map(f -> f.field() + "|" + f.type())
                .collect(Collectors.toSet());
        return detectFlags.stream()
                .map(f -> stripped.contains(f.field() + "|" + f.type()) ? f.withAction("STRIPPED") : f)
                .collect(Collectors.toList());
    }

    /**
     * Builds a Korean corrective instruction listing the HARD violations, to append to the
     * LLM user message on the self-repair retry.
     */
    public String buildRepairFeedback(List<HarnessFlag> flags) {
        String issues = flags.stream()
                .filter(f -> f.severity() == Severity.HARD)
                .map(f -> "- " + f.field() + ": " + f.message())
                .distinct()
                .collect(Collectors.joining("\n"));

        return "\n\n[중요 — 직전 응답 검증 실패, 아래를 반드시 고쳐 다시 JSON만 출력하세요]\n"
                + issues
                + "\n규칙: 전화번호/링크를 임의로 만들지 말 것(모르면 '공식 홈페이지 확인'이라고 쓸 것), "
                + "본문에 금액(원/만원/억)을 쓰지 말 것(금액은 시스템이 별도 계산함), "
                + "제공된 '적용 가능 제도' 범위를 벗어난 제도를 언급하지 말 것.";
    }
}
