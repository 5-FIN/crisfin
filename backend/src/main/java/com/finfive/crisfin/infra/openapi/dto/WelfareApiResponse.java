package com.finfive.crisfin.infra.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Maps the XML response of the LocalGovernment welfare API
 * ({@code /LcgvWelfarelist}). The root element is {@code <wantedList>} and each
 * benefit is a {@code <servList>} element repeated directly under the root
 * (no wrapping container).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "wantedList")
public class WelfareApiResponse {

    @JacksonXmlProperty(localName = "totalCount")
    private int totalCount;

    @JacksonXmlProperty(localName = "pageNo")
    private int pageNo;

    @JacksonXmlProperty(localName = "numOfRows")
    private int numOfRows;

    @JacksonXmlProperty(localName = "resultCode")
    private String resultCode;

    @JacksonXmlProperty(localName = "resultMessage")
    private String resultMessage;

    /**
     * The benefit items. {@code <servList>} elements repeat directly under
     * {@code <wantedList>} without a wrapper, so wrapping is disabled.
     */
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "servList")
    private List<WelfareItem> servList;

    /** Returns an empty response (no items) used when the API key is absent. */
    public static WelfareApiResponse empty() {
        WelfareApiResponse response = new WelfareApiResponse();
        response.totalCount = 0;
        response.pageNo = 0;
        response.numOfRows = 0;
        response.servList = Collections.emptyList();
        return response;
    }

    /** Returns the benefit items, or an empty list when none were parsed. */
    public List<WelfareItem> getData() {
        return servList != null ? servList : Collections.emptyList();
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WelfareItem {

        /** 서비스 ID */
        @JacksonXmlProperty(localName = "servId")
        private String servId;

        /** 서비스명 */
        @JacksonXmlProperty(localName = "servNm")
        private String servNm;

        /** 서비스 요약 */
        @JacksonXmlProperty(localName = "servDgst")
        private String servDgst;

        /** 신청 방법명 */
        @JacksonXmlProperty(localName = "aplyMtdNm")
        private String aplyMtdNm;

        /** 담당 부서명 (소관기관) */
        @JacksonXmlProperty(localName = "bizChrDeptNm")
        private String bizChrDeptNm;

        /** 시도명 */
        @JacksonXmlProperty(localName = "ctpvNm")
        private String ctpvNm;

        /** 시군구명 */
        @JacksonXmlProperty(localName = "sggNm")
        private String sggNm;

        /** 상세 링크 URL */
        @JacksonXmlProperty(localName = "servDtlLink")
        private String servDtlLink;

        /** 관심 주제 배열 (예: 서민금융) */
        @JacksonXmlProperty(localName = "intrsThemaNmArray")
        private String intrsThemaNmArray;

        /** 생애주기 배열 (예: 청년) */
        @JacksonXmlProperty(localName = "lifeNmArray")
        private String lifeNmArray;
    }
}
