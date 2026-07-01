package com.finfive.crisfin.infra.openapi.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.finfive.crisfin.infra.openapi.dto.WelfareApiResponse.WelfareItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XML 파싱 단위 테스트 — 실제 LocalGovernment 복지 API 응답 구조를
 * {@link XmlMapper}로 {@link WelfareApiResponse}에 매핑하는지 검증한다.
 */
class WelfareApiResponseTest {

    private static final String SAMPLE_XML = """
            <wantedList>
              <totalCount>4600</totalCount>
              <pageNo>1</pageNo>
              <numOfRows>100</numOfRows>
              <resultCode>0</resultCode>
              <resultMessage>SUCCESS</resultMessage>
              <servList>
                <servId>WLF00000001</servId>
                <servNm>청년 서민금융 지원</servNm>
                <servDgst>저소득 청년 대상 금융 지원 요약</servDgst>
                <aplyMtdNm>방문</aplyMtdNm>
                <bizChrDeptNm>강원특별자치도 철원군 인구정책과</bizChrDeptNm>
                <ctpvNm>강원특별자치도</ctpvNm>
                <sggNm>철원군</sggNm>
                <servDtlLink>https://example.gov/detail/1</servDtlLink>
                <intrsThemaNmArray>서민금융</intrsThemaNmArray>
                <lifeNmArray>청년</lifeNmArray>
                <sprtCycNm>수시</sprtCycNm>
                <srvPvsnNm>현금</srvPvsnNm>
                <inqNum>32</inqNum>
                <lastModYmd>20260630</lastModYmd>
              </servList>
              <servList>
                <servId>WLF00000002</servId>
                <servNm>어르신 돌봄 서비스</servNm>
                <servDgst>고령층 돌봄 요약</servDgst>
                <aplyMtdNm>온라인</aplyMtdNm>
                <bizChrDeptNm>서울특별시 종로구 복지정책과</bizChrDeptNm>
                <ctpvNm>서울특별시</ctpvNm>
                <sggNm>종로구</sggNm>
                <servDtlLink>https://example.gov/detail/2</servDtlLink>
                <intrsThemaNmArray>노인</intrsThemaNmArray>
                <lifeNmArray>노년</lifeNmArray>
                <inqNum>10</inqNum>
                <lastModYmd>20260629</lastModYmd>
              </servList>
            </wantedList>
            """;

    @Test
    void parsesWantedListXml() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        WelfareApiResponse response = xmlMapper.readValue(SAMPLE_XML, WelfareApiResponse.class);

        assertThat(response.getTotalCount()).isEqualTo(4600);
        assertThat(response.getPageNo()).isEqualTo(1);
        assertThat(response.getNumOfRows()).isEqualTo(100);
        assertThat(response.getResultCode()).isEqualTo("0");
        assertThat(response.getResultMessage()).isEqualTo("SUCCESS");

        List<WelfareItem> items = response.getData();
        assertThat(items).hasSize(2);

        WelfareItem first = items.get(0);
        assertThat(first.getServId()).isEqualTo("WLF00000001");
        assertThat(first.getServNm()).isEqualTo("청년 서민금융 지원");
        assertThat(first.getServDgst()).isEqualTo("저소득 청년 대상 금융 지원 요약");
        assertThat(first.getAplyMtdNm()).isEqualTo("방문");
        assertThat(first.getBizChrDeptNm()).isEqualTo("강원특별자치도 철원군 인구정책과");
        assertThat(first.getCtpvNm()).isEqualTo("강원특별자치도");
        assertThat(first.getSggNm()).isEqualTo("철원군");
        assertThat(first.getServDtlLink()).isEqualTo("https://example.gov/detail/1");
        assertThat(first.getIntrsThemaNmArray()).isEqualTo("서민금융");
        assertThat(first.getLifeNmArray()).isEqualTo("청년");

        WelfareItem second = items.get(1);
        assertThat(second.getServId()).isEqualTo("WLF00000002");
        assertThat(second.getCtpvNm()).isEqualTo("서울특별시");
        assertThat(second.getSggNm()).isEqualTo("종로구");
    }

    @Test
    void emptyResponseReturnsEmptyData() {
        WelfareApiResponse empty = WelfareApiResponse.empty();

        assertThat(empty.getData()).isEmpty();
        assertThat(empty.getTotalCount()).isZero();
    }
}
