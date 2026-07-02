package com.finfive.crisfin.domain.recommendation.harness;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Flags overconfident/absolute wording the output contract forbids ("반드시", "확실히",
 * "무조건", "보장", "100%"). SOFT severity — the content is kept but surfaced so the UI can
 * temper it, since financial outcomes are never guaranteed.
 */
@Component
public class PhrasingVerifier implements OutputVerifier {

    private static final Set<String> NARRATIVE_KEYS =
            Set.of("action", "reason", "name", "howTo", "caution", "thirtyDayPlan");

    private static final Pattern OVERCONFIDENT =
            Pattern.compile("반드시|확실히|무조건|보장|절대|100\\s*%");

    @Override
    public List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();
        TextTraversal.forEachFreeText(resultMap, (path, key, value, replacer) -> {
            if (NARRATIVE_KEYS.contains(key) && OVERCONFIDENT.matcher(value).find()) {
                flags.add(HarnessFlag.soft(path, "OVERCONFIDENT",
                        "단정적 표현이 사용되었습니다. 실제 수급은 자격 심사 결과에 따라 달라질 수 있습니다."));
            }
        });
        return flags;
    }
}
