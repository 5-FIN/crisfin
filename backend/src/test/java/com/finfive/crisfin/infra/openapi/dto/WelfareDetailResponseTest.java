package com.finfive.crisfin.infra.openapi.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상세조회 API 응답({@code <wantedDtl>}) 파싱 및 {@link WelfareDetailResponse#toDetailContent()}
 * 라벨링 로직 단위 테스트.
 */
class WelfareDetailResponseTest {

    private static WelfareDetailResponse parse(String xml) throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return xmlMapper.readValue(xml, WelfareDetailResponse.class);
    }

    @Test
    void toDetailContentBuildsLabeledStringFromSetFields() throws Exception {
        String xml = """
                <wantedDtl>
                  <resultCode>0</resultCode>
                  <servId>WLF00000001</servId>
                  <servNm>청년 서민금융 지원</servNm>
                  <servDgst>저소득 청년의 금융 자립을 돕는 서비스</servDgst>
                  <sprtTrgtCn>만 19~34세 저소득 청년</sprtTrgtCn>
                  <slctCritCn>중위소득 50% 이하</slctCritCn>
                  <alwServCn>월 30만원 지원</alwServCn>
                  <aplyMtdCn>주민센터 방문 신청</aplyMtdCn>
                  <sprtCycNm>월</sprtCycNm>
                  <srvPvsnNm>현금</srvPvsnNm>
                  <bizChrDeptNm>강원특별자치도 철원군 인구정책과</bizChrDeptNm>
                </wantedDtl>
                """;

        WelfareDetailResponse detail = parse(xml);

        assertThat(detail.getResultCode()).isEqualTo("0");
        assertThat(detail.getServId()).isEqualTo("WLF00000001");

        String content = detail.toDetailContent();
        assertThat(content).contains("[개요] 저소득 청년의 금융 자립을 돕는 서비스");
        assertThat(content).contains("[지원대상] 만 19~34세 저소득 청년");
        assertThat(content).contains("[선정기준] 중위소득 50% 이하");
        assertThat(content).contains("[지원내용] 월 30만원 지원");
        assertThat(content).contains("[신청방법] 주민센터 방문 신청");
    }

    @Test
    void toDetailContentSkipsBlankFields() throws Exception {
        String xml = """
                <wantedDtl>
                  <resultCode>0</resultCode>
                  <servId>WLF00000002</servId>
                  <servDgst>개요만 있는 서비스</servDgst>
                  <alwServCn>월 10만원 지원</alwServCn>
                </wantedDtl>
                """;

        WelfareDetailResponse detail = parse(xml);
        String content = detail.toDetailContent();

        assertThat(content).contains("[개요] 개요만 있는 서비스");
        assertThat(content).contains("[지원내용] 월 10만원 지원");
        assertThat(content).doesNotContain("[지원대상]");
        assertThat(content).doesNotContain("[선정기준]");
        assertThat(content).doesNotContain("[신청방법]");
    }

    @Test
    void toDetailContentReturnsEmptyWhenAllBlank() throws Exception {
        String xml = """
                <wantedDtl>
                  <resultCode>0</resultCode>
                  <servId>WLF00000003</servId>
                  <servNm>이름만 있는 서비스</servNm>
                </wantedDtl>
                """;

        WelfareDetailResponse detail = parse(xml);

        assertThat(detail.toDetailContent()).isEmpty();
    }
}
