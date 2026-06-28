package com.finfive.crisfin.domain.crisis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the category of a financial crisis that a user may be experiencing.
 *
 * <ul>
 *   <li>{@code label}       – Human-readable Korean label shown in the UI.</li>
 *   <li>{@code recurring}   – {@code true} when the situation tends to be prolonged /
 *       gradual (e.g. caregiving, unemployment) rather than a single acute event.</li>
 *   <li>{@code emoji}       – Emoji used in UI cards and notifications.</li>
 *   <li>{@code description} – Short Korean description of the crisis category.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum CrisisType {

    HOSPITALIZATION(
            "입원/수술",
            false,
            "🏥",
            "갑작스러운 입원이나 수술로 인한 의료비 및 소득 공백 위기"
    ),
    ACCIDENT(
            "사고/재해",
            false,
            "🚑",
            "사고·자연재해로 발생한 긴급 재정 위기"
    ),
    UNEMPLOYMENT(
            "실직/소득단절",
            true,
            "💼",
            "실직 또는 지속적인 소득 감소로 인한 생계 위기"
    ),
    CAREGIVING(
            "간병",
            true,
            "🤲",
            "가족 구성원의 장기 간병으로 인한 경제적 부담"
    ),
    BEREAVEMENT(
            "가족 사망",
            false,
            "🕊️",
            "가족의 갑작스러운 사망으로 인한 재정·심리적 위기"
    );

    private final String label;
    private final boolean recurring;
    private final String emoji;
    private final String description;
}
