package com.finfive.crisfin.domain.recommendation.harness;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Flags absolute calendar dates in deadline/dayRange fields. The contract asks for relative
 * timing (e.g. "D+3", "3일 이내"); an absolute date such as "2023-10-15" is almost always a
 * fabricated/stale value that misleads the user about real deadlines. SOFT — flag only.
 */
@Component
public class DateVerifier implements OutputVerifier {

    private static final Set<String> DATE_KEYS = Set.of("deadline", "dayRange");

    // YYYY-MM-DD / YYYY.MM.DD / YYYY년 형태의 절대 날짜
    private static final Pattern ABSOLUTE_DATE =
            Pattern.compile("\\b(19|20)\\d{2}\\s*[-.년]\\s*\\d{1,2}");

    @Override
    public List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();
        TextTraversal.forEachFreeText(resultMap, (path, key, value, replacer) -> {
            if (DATE_KEYS.contains(key) && ABSOLUTE_DATE.matcher(value).find()) {
                flags.add(HarnessFlag.soft(path, "ABSOLUTE_DATE",
                        "절대 날짜가 사용되었습니다. 실제 신청 기한은 상황 발생일 기준으로 다시 확인하세요."));
            }
        });
        return flags;
    }
}
