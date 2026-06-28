package com.finfive.crisfin.domain.guide.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GuideResponse {

    private final String crisisType;
    private final String title;
    private final String coachingPrompt;
    private final List<String> keyRules;
    private final List<String> sourceLaws;

    @Builder.Default
    private final String disclaimer = "본 정보는 참고용이며 전문가 상담을 권장합니다.";
}
