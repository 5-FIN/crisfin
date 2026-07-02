package com.finfive.crisfin.domain.recommendation.harness;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flags monetary figures that leak into the LLM's narrative fields. All amounts must come
 * from the deterministic rule engine (surfaced under {@code receivable}/{@code summary});
 * a won figure asserted inside a todo/action/plan is an ungrounded amount claim — a HARD
 * violation. On sanitize the figure is redacted, pointing the user to the authoritative
 * "받을 돈" section.
 */
@Component
public class AmountLeakVerifier implements OutputVerifier {

    /** Only narrative fields — date-like fields (deadline/dayRange) are excluded. */
    private static final Set<String> NARRATIVE_KEYS =
            Set.of("action", "reason", "name", "howTo", "caution", "thirtyDayPlan");

    // 숫자 + 통화 단위(원/만원/억). 날짜(개월/일)·비율(%)은 매칭되지 않는다.
    private static final Pattern WON = Pattern.compile("\\d[\\d,]*\\s*(억\\s*원?|만\\s*원|원)");

    private static final String REDACTED = "[금액은 '받을 돈' 항목 참고]";

    @Override
    public List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();

        TextTraversal.forEachFreeText(resultMap, (path, key, value, replacer) -> {
            if (!NARRATIVE_KEYS.contains(key)) {
                return;
            }
            Matcher m = WON.matcher(value);
            if (m.find()) {
                flags.add(new HarnessFlag(path, "AMOUNT_LEAK", Severity.HARD,
                        "금액은 규칙 엔진이 산정한 '받을 돈'에만 표시됩니다. 본문에서 단언된 금액을 제거했습니다.",
                        sanitize ? "STRIPPED" : "FLAGGED"));
                if (sanitize) {
                    replacer.accept(m.reset().replaceAll(REDACTED));
                }
            }
        });

        return flags;
    }
}
