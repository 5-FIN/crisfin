package com.finfive.crisfin.domain.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request payload for the AI-powered financial crisis analysis endpoint.
 *
 * <p>{@code crisisType}            – Name of the crisis category (maps to {@link com.finfive.crisfin.domain.crisis.CrisisType}).</p>
 * <p>{@code situationDescription}  – Free-text description of the user's situation (max 500 chars).</p>
 * <p>{@code filteredMyData}        – Optional pre-filtered MyData key/value pairs forwarded to the LLM prompt.</p>
 */
@Getter
@NoArgsConstructor
public class AnalysisRequest {

    @NotBlank(message = "위기 유형은 필수입니다.")
    @NotNull
    private String crisisType;

    @NotBlank(message = "상황 설명은 필수입니다.")
    @Size(max = 500, message = "상황 설명은 500자를 초과할 수 없습니다.")
    private String situationDescription;

    private Map<String, Object> filteredMyData;

    /** Optional structured inputs for the benefit rule engine (자격·금액 계산). */
    private ApplicantProfile applicantProfile;
}
