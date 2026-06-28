package com.finfive.crisfin.domain.crisis.dto;

import com.finfive.crisfin.domain.crisis.CrisisType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrisisTypeResponse {

    private final String type;
    private final String label;
    private final String emoji;
    private final String description;
    private final boolean isRecurring;

    public static CrisisTypeResponse from(CrisisType t) {
        return CrisisTypeResponse.builder()
                .type(t.name())
                .label(t.getLabel())
                .emoji(t.getEmoji())
                .description(t.getDescription())
                .isRecurring(t.isRecurring())
                .build();
    }
}
