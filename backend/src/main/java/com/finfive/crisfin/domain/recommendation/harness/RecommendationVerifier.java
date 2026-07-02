package com.finfive.crisfin.domain.recommendation.harness;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Flags language that reads as soliciting a specific financial product ("이 대출을 신청하세요",
 * "이 보험이 가장 좋습니다"). Recommending products to users in crisis carries advertising /
 * brokerage / advisory legal risk (security spec, slide 10); the safe framing is information
 * checking ("상환 유예 가능 여부를 확인하세요", "보장 범위를 확인하세요").
 *
 * <p>SOFT — the sentence is kept but surfaced with a caveat rather than deleted, since
 * regex can't perfectly separate solicitation from legitimate debt-management advice.
 * Sentences about deferral/adjustment/reduction of <em>existing</em> obligations are treated
 * as the safe direction and skipped.</p>
 */
@Component
public class RecommendationVerifier implements OutputVerifier {

    private static final Set<String> NARRATIVE_KEYS =
            Set.of("action", "name", "reason", "howTo", "caution", "thirtyDayPlan");

    // 특정 금융상품 가입/신청 권유 또는 우위 단언
    private static final Pattern SOLICIT = Pattern.compile(
            "(대출|보험|카드|적금|펀드|투자상품)[을를이가에]?\\s*(가입|신청|추천|드세요|가장\\s*좋|최고|제일\\s*좋)");

    // 기존 의무의 유예·조정 등 '정보 확인' 방향(안전)이면 권유로 보지 않는다
    private static final Pattern SAFE_CONTEXT =
            Pattern.compile("유예|조정|상환|연체|해지|감면|리스케|리스케줄");

    @Override
    public List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();
        TextTraversal.forEachFreeText(resultMap, (path, key, value, replacer) -> {
            if (!NARRATIVE_KEYS.contains(key)) {
                return;
            }
            if (SOLICIT.matcher(value).find() && !SAFE_CONTEXT.matcher(value).find()) {
                flags.add(HarnessFlag.soft(path, "PRODUCT_SOLICITATION",
                        "특정 금융상품 권유로 해석될 수 있는 표현입니다. 가입 권유가 아닌 "
                        + "'상환 유예·보장 범위·공공지원 확인' 관점의 정보 안내인지 확인하세요."));
            }
        });
        return flags;
    }
}
