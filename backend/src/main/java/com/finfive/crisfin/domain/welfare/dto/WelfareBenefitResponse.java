package com.finfive.crisfin.domain.welfare.dto;

import com.finfive.crisfin.domain.welfare.WelfareBenefit;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class WelfareBenefitResponse {

    private final Long id;
    private final String serviceName;
    private final String summary;
    private final String targetDescription;
    private final String applyUrl;
    private final String ministryName;
    private final String contact;
    private final List<String> crisisTags;
    private final String externalServiceId;
    private final String selectionCriteria;
    private final String applyMethod;
    private final boolean isActive;
    private final LocalDateTime lastSyncedAt;

    public static WelfareBenefitResponse from(WelfareBenefit benefit) {
        return WelfareBenefitResponse.builder()
                .id(benefit.getId())
                .serviceName(benefit.getServiceName())
                .summary(benefit.getSummary())
                .targetDescription(benefit.getTargetDescription())
                .applyUrl(benefit.getApplyUrl())
                .ministryName(benefit.getMinistryName())
                .contact(benefit.getContact())
                .crisisTags(benefit.getCrisisTags())
                .externalServiceId(benefit.getExternalServiceId())
                .selectionCriteria(benefit.getSelectionCriteria())
                .applyMethod(benefit.getApplyMethod())
                .isActive(benefit.isActive())
                .lastSyncedAt(benefit.getLastSyncedAt())
                .build();
    }
}
