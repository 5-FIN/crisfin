package com.finfive.crisfin.domain.mydata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Describes the employment / income profile of a user.
 * Used during onboarding and for persona-tailored guide recommendations.
 */
@Getter
@RequiredArgsConstructor
public enum PersonaType {

    OFFICE_WORKER("직장인"),
    SELF_EMPLOYED("자영업자"),
    FREELANCER("프리랜서"),
    LAID_OFF("권고사직자"),
    PUBLIC_SERVANT("공무원");

    private final String label;
}
