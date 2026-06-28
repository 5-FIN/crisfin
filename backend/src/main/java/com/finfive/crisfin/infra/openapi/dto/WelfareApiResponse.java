package com.finfive.crisfin.infra.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WelfareApiResponse {

    private int currentCount;
    private int totalCount;
    private List<WelfareItem> data;

    /** Returns an empty response (no items) used when the API key is absent. */
    public static WelfareApiResponse empty() {
        WelfareApiResponse response = new WelfareApiResponse();
        response.currentCount = 0;
        response.totalCount = 0;
        response.data = Collections.emptyList();
        return response;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WelfareItem {

        /** 서비스 ID */
        private String svcId;

        /** 서비스명 */
        private String srvNm;

        /** 서비스 요약 */
        private String svcSumry;

        /** 지원대상 상세 내용 */
        private String tgtrDtlCn;

        /** 선정기준 구분 코드 */
        private String slctCrtDvCd;

        /** 신청방법 내용 */
        private String aplyMtdCn;

        /** 서비스 URL */
        private String svcUrl;

        /** 소관기관명 */
        private String jrsdInstNm;
    }
}
