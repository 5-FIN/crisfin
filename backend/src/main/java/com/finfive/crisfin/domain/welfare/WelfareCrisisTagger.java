package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.domain.crisis.CrisisType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based classifier that maps welfare-service text (name/summary/theme/life-cycle)
 * to CrisFin {@link CrisisType} tags via keyword matching.
 *
 * <p>The LocalGovernment welfare API does not expose our crisis categories — it provides
 * 관심주제(theme)·생애주기(life cycle) instead. This tagger derives crisis tags so synced
 * welfare can be filtered by "내 위기". An item may match multiple crises (e.g. a disabled
 * medical-support program → CAREGIVING + HOSPITALIZATION); non-matching items get no tags
 * and are still discoverable by region.</p>
 */
@Component
public class WelfareCrisisTagger {

    /** CrisisType → 매칭 키워드. 삽입 순서를 유지해 태그 순서를 안정적으로 만든다. */
    private static final Map<CrisisType, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(CrisisType.HOSPITALIZATION,
                List.of("의료", "입원", "수술", "질병", "치료", "건강", "병원", "진료", "의료비", "암", "재활"));
        KEYWORDS.put(CrisisType.ACCIDENT,
                List.of("사고", "재해", "재난", "산재", "부상", "피해"));
        KEYWORDS.put(CrisisType.UNEMPLOYMENT,
                List.of("실업", "구직", "취업", "일자리", "고용", "실직", "자활", "재취업", "근로자"));
        KEYWORDS.put(CrisisType.CAREGIVING,
                List.of("간병", "돌봄", "요양", "장애", "노인", "장기요양", "보호"));
        KEYWORDS.put(CrisisType.BEREAVEMENT,
                List.of("사망", "유족", "상속", "장례", "장제", "유가족"));
    }

    /**
     * Returns the crisis-type tags (as enum names) whose keywords appear in any of the
     * supplied text fields. Nulls are ignored; an item matching no keyword returns an
     * empty list.
     */
    public List<String> tag(String... texts) {
        StringBuilder sb = new StringBuilder();
        for (String t : texts) {
            if (t != null) {
                sb.append(t).append(' ');
            }
        }
        String haystack = sb.toString();

        List<String> tags = new ArrayList<>();
        for (Map.Entry<CrisisType, List<String>> e : KEYWORDS.entrySet()) {
            for (String kw : e.getValue()) {
                if (haystack.contains(kw)) {
                    tags.add(e.getKey().name());
                    break;
                }
            }
        }
        return tags;
    }
}
