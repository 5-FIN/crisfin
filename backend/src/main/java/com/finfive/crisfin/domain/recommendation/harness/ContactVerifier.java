package com.finfive.crisfin.domain.recommendation.harness;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flags fabricated contact channels — the most dangerous financial hallucination, since a
 * user may call a wrong number or submit personal/financial data to a fake site.
 *
 * <p>Phone numbers appearing in {@code contactInfo} are checked against a curated allowlist
 * of official Korean crisis/finance hotlines. URLs anywhere in the free text must resolve to
 * a government/public domain ({@code .go.kr}, {@code .or.kr}). Anything else is a HARD
 * violation; on sanitize the offending value is replaced with a safe placeholder.</p>
 */
@Component
public class ContactVerifier implements OutputVerifier {

    /** Official hotlines relevant to crisis-finance. Normalised to digits-only for matching. */
    private static final Set<String> ALLOWED_PHONES = Set.of(
            "129",       // 보건복지상담센터 / 긴급복지
            "1350",      // 고용노동부 고객상담
            "1355",      // 국민연금공단
            "1577-1000", // 국민건강보험공단
            "1588-0075", // 근로복지공단(산재)
            "1544-9000", // 근로복지공단
            "110",       // 정부민원안내콜센터
            "120",       // 지자체 민원
            "112", "119" // 긴급
    );

    /** Government / public-institution domains a legitimate applyUrl/link would use. */
    private static final List<String> ALLOWED_URL_HOSTS = List.of(
            ".go.kr", ".or.kr"
    );

    // 국내 전화번호 유사 토큰: 국번-3~4-4, 4자리 국번-4, 3자리 특수번호
    private static final Pattern PHONE = Pattern.compile(
            "\\b(0\\d{1,2}-\\d{3,4}-\\d{4}|1\\d{3}-\\d{4}|\\d{3,4}-\\d{4}|1\\d{2})\\b");
    private static final Pattern URL = Pattern.compile("https?://[^\\s\"'()]+", Pattern.CASE_INSENSITIVE);

    private static final String SAFE_CONTACT = "정확한 연락처는 해당 기관 공식 홈페이지에서 확인하세요";

    @Override
    public List<HarnessFlag> inspect(java.util.Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();

        TextTraversal.forEachFreeText(resultMap, (path, key, value, replacer) -> {
            boolean badPhone = "contactInfo".equals(key) && hasUnknownPhone(value);
            boolean badUrl = hasUntrustedUrl(value);

            if (badPhone) {
                flags.add(new HarnessFlag(path, "FABRICATED_CONTACT", Severity.HARD,
                        "검증되지 않은 전화번호가 감지되어 제거했습니다. 공식 기관에 직접 확인하세요.",
                        sanitize ? "STRIPPED" : "FLAGGED"));
            }
            if (badUrl) {
                flags.add(new HarnessFlag(path, "FABRICATED_URL", Severity.HARD,
                        "공공기관(.go.kr/.or.kr) 외 링크가 감지되어 제거했습니다.",
                        sanitize ? "STRIPPED" : "FLAGGED"));
            }

            if (sanitize && (badPhone || badUrl)) {
                if (badPhone) {
                    replacer.accept(SAFE_CONTACT);
                } else {
                    replacer.accept(URL.matcher(value).replaceAll("[링크 제거됨]"));
                }
            }
        });

        return flags;
    }

    private boolean hasUnknownPhone(String value) {
        Matcher m = PHONE.matcher(value);
        while (m.find()) {
            if (!ALLOWED_PHONES.contains(m.group(1))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUntrustedUrl(String value) {
        Matcher m = URL.matcher(value);
        while (m.find()) {
            String url = m.group().toLowerCase();
            boolean trusted = ALLOWED_URL_HOSTS.stream().anyMatch(host ->
                    url.contains(host + "/") || url.endsWith(host) || url.contains(host + "?"));
            if (!trusted) {
                return true;
            }
        }
        return false;
    }
}
