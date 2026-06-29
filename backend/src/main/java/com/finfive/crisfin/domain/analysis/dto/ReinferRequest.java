package com.finfive.crisfin.domain.analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for {@code POST /api/v1/analysis/{id}/reinfer}.
 *
 * <p>Every field is optional — omitted fields are inherited from the parent analysis, so a
 * client can re-run a personalised analysis by overriding only what changed.</p>
 */
@Getter
@NoArgsConstructor
public class ReinferRequest {

    private Map<String, Object> filteredMyData;
    private ApplicantProfile applicantProfile;
    private String situationDescription;
}
