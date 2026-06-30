package com.finfive.crisfin.domain.mydata;

import com.finfive.crisfin.domain.mydata.dto.MyDataFilterRequest;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MyDataService} — 페르소나 파싱 + 필드 필터링.
 */
@ExtendWith(MockitoExtension.class)
class MyDataServiceTest {

    @Mock
    private MockMyDataRepository mockMyDataRepository;

    @InjectMocks
    private MyDataService myDataService;

    private Map<String, Object> sampleData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cards", List.of(Map.of("issuer", "A")));
        data.put("loans", List.of(Map.of("lender", "B")));
        data.put("insurance", List.of(Map.of("productName", "C")));
        return data;
    }

    @Test
    void getMockData_validPersona_returnsFinancialData() {
        MockMyDataProfile profile = mock(MockMyDataProfile.class);
        when(profile.getFinancialData()).thenReturn(sampleData());
        when(mockMyDataRepository.findByPersona(PersonaType.OFFICE_WORKER))
                .thenReturn(Optional.of(profile));

        Map<String, Object> result = myDataService.getMockData("OFFICE_WORKER");

        assertThat(result).containsKeys("cards", "loans", "insurance");
    }

    @Test
    void getMockData_invalidPersonaString_throwsPersonaNotFound() {
        assertThatThrownBy(() -> myDataService.getMockData("NOT_A_PERSONA"))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONA_NOT_FOUND);
    }

    @Test
    void getMockData_personaNotInDb_throwsPersonaNotFound() {
        when(mockMyDataRepository.findByPersona(PersonaType.FREELANCER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> myDataService.getMockData("FREELANCER"))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONA_NOT_FOUND);
    }

    @Test
    void filterData_returnsOnlySelectedExistingFields() {
        MockMyDataProfile profile = mock(MockMyDataProfile.class);
        when(profile.getFinancialData()).thenReturn(sampleData());
        when(mockMyDataRepository.findByPersona(PersonaType.OFFICE_WORKER))
                .thenReturn(Optional.of(profile));

        MyDataFilterRequest req = mock(MyDataFilterRequest.class);
        when(req.getPersona()).thenReturn("OFFICE_WORKER");
        // 'nope'는 존재하지 않는 키 → 결과에서 제외돼야 함
        when(req.getSelectedFields()).thenReturn(List.of("cards", "loans", "nope"));

        Map<String, Object> result = myDataService.filterData(req);

        assertThat(result).containsOnlyKeys("cards", "loans");
    }
}
