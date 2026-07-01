package com.finfive.crisfin.infra.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * Maps the XML response of the LocalGovernment welfare <em>detail</em> API
 * ({@code /LcgvWelfaredetailed}). The root element is {@code <wantedDtl>} and it
 * carries the full service text (개요·지원대상·선정기준·지원내용·신청방법 등) used as
 * the RAG document-ingest source.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "wantedDtl")
public class WelfareDetailResponse {

    @JacksonXmlProperty(localName = "resultCode")
    private String resultCode;

    /** 서비스 ID */
    @JacksonXmlProperty(localName = "servId")
    private String servId;

    /** 서비스명 */
    @JacksonXmlProperty(localName = "servNm")
    private String servNm;

    /** 서비스 개요/취지 */
    @JacksonXmlProperty(localName = "servDgst")
    private String servDgst;

    /** 지원대상 */
    @JacksonXmlProperty(localName = "sprtTrgtCn")
    private String sprtTrgtCn;

    /** 선정기준 */
    @JacksonXmlProperty(localName = "slctCritCn")
    private String slctCritCn;

    /** 지원내용 (금액 등) */
    @JacksonXmlProperty(localName = "alwServCn")
    private String alwServCn;

    /** 신청방법 */
    @JacksonXmlProperty(localName = "aplyMtdCn")
    private String aplyMtdCn;

    /** 지원주기 */
    @JacksonXmlProperty(localName = "sprtCycNm")
    private String sprtCycNm;

    /** 제공방법/형태 */
    @JacksonXmlProperty(localName = "srvPvsnNm")
    private String srvPvsnNm;

    /** 소관부서 */
    @JacksonXmlProperty(localName = "bizChrDeptNm")
    private String bizChrDeptNm;

    /**
     * Builds a single labeled string from the non-blank rich fields, used as the
     * RAG ingest source. Each present field is emitted as {@code [라벨] 내용} on its
     * own line; blank fields (and their labels) are skipped. Returns {@code ""}
     * when every field is blank.
     */
    public String toDetailContent() {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "개요", servDgst);
        appendField(sb, "지원대상", sprtTrgtCn);
        appendField(sb, "선정기준", slctCritCn);
        appendField(sb, "지원내용", alwServCn);
        appendField(sb, "신청방법", aplyMtdCn);
        return sb.toString();
    }

    /** Appends {@code [label] value} on a new line when {@code value} is non-blank (whitespace-normalized). */
    private static void appendField(StringBuilder sb, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append('[').append(label).append("] ").append(normalized);
    }
}
