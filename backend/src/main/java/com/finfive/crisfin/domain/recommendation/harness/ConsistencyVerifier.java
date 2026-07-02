package com.finfive.crisfin.domain.recommendation.harness;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cross-checks the LLM narrative against the rule engine's eligibility findings.
 *
 * <p>When the rule engine marks a benefit as "추가 입력 필요"(eligibility not yet
 * determinable) but the LLM's todos/actions reference that benefit, the narrative may be
 * over-promising a benefit the user might not actually qualify for. This is a SOFT flag —
 * the advice is kept but the UI can attach an "자격 추가 확인 필요" caveat, since telling the
 * user to apply is not wrong, only uncertain.</p>
 */
@Component
public class ConsistencyVerifier implements OutputVerifier {

    private static final Set<String> NARRATIVE_KEYS = Set.of("action", "name", "reason");

    @Override
    public List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();
        Set<String> needsMoreInput = ctx.needsMoreInput();
        if (needsMoreInput == null || needsMoreInput.isEmpty()) {
            return flags;
        }

        TextTraversal.forEachFreeText(resultMap, (path, key, value, replacer) -> {
            if (!NARRATIVE_KEYS.contains(key)) {
                return;
            }
            for (String benefit : needsMoreInput) {
                if (benefit != null && !benefit.isBlank() && value.contains(benefit)) {
                    flags.add(HarnessFlag.soft(path, "ELIGIBILITY_UNCERTAIN",
                            "'" + benefit + "'은(는) 자격 추가 확인이 필요한 제도입니다. 수급이 확정된 것으로 안내하지 마세요."));
                    break; // 항목당 한 번만
                }
            }
        });
        return flags;
    }
}
