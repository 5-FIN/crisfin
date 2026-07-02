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
    private final String ctpvNm;
    private final String sggNm;
    private final List<String> crisisTags;
    private final String externalServiceId;
    private final String selectionCriteria;
    private final String applyMethod;
    private final boolean isActive;
    private final LocalDateTime lastSyncedAt;
    /** 상세조회 본문(개요·지원대상·선정기준·지원내용·신청방법). 목록에선 null, 상세에서만 채운다. */
    private final String detailContent;

    /** 목록용 매핑 — 상세 본문(detailContent)은 제외해 응답을 가볍게 유지한다. */
    public static WelfareBenefitResponse from(WelfareBenefit benefit) {
        return baseBuilder(benefit).build();
    }

    /** 상세용 매핑 — 상세 본문까지 포함한다. */
    public static WelfareBenefitResponse fromDetail(WelfareBenefit benefit) {
        return baseBuilder(benefit)
                .detailContent(benefit.getDetailContent())
                .build();
    }

    private static WelfareBenefitResponseBuilder baseBuilder(WelfareBenefit benefit) {
        return WelfareBenefitResponse.builder()
                .id(benefit.getId())
                .serviceName(benefit.getServiceName())
                .summary(benefit.getSummary())
                .targetDescription(benefit.getTargetDescription())
                .applyUrl(benefit.getApplyUrl())
                .ministryName(benefit.getMinistryName())
                .contact(benefit.getContact())
                .ctpvNm(benefit.getCtpvNm())
                .sggNm(benefit.getSggNm())
                .crisisTags(benefit.getCrisisTags())
                .externalServiceId(benefit.getExternalServiceId())
                .selectionCriteria(benefit.getSelectionCriteria())
                .applyMethod(benefit.getApplyMethod())
                .isActive(benefit.isActive())
                .lastSyncedAt(benefit.getLastSyncedAt());
    }
}
