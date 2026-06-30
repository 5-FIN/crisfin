package com.finfive.crisfin.domain.guide;

import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.domain.guide.dto.GuideResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GuideService} — 위기유형 파싱 + GuideResponse 매핑.
 */
@ExtendWith(MockitoExtension.class)
class GuideServiceTest {

    @Mock
    private CrisisGuideRepository crisisGuideRepository;

    @InjectMocks
    private GuideService guideService;

    private CrisisGuide guideStub() {
        CrisisGuide guide = mock(CrisisGuide.class);
        when(guide.getCrisisType()).thenReturn(CrisisType.UNEMPLOYMENT);
        when(guide.getTitle()).thenReturn("실직 길라잡이");
        when(guide.getCoachingPrompt()).thenReturn("프롬프트");
        when(guide.getKeyRules()).thenReturn(List.of("수칙1", "수칙2"));
        when(guide.getSourceLaws()).thenReturn(List.of("고용보험법"));
        return guide;
    }

    @Test
    void getGuide_validType_mapsToResponse() {
        when(crisisGuideRepository.findByCrisisType(CrisisType.UNEMPLOYMENT))
                .thenReturn(Optional.of(guideStub()));

        GuideResponse res = guideService.getGuide("UNEMPLOYMENT");

        assertThat(res.getCrisisType()).isEqualTo("UNEMPLOYMENT");
        assertThat(res.getTitle()).isEqualTo("실직 길라잡이");
        assertThat(res.getKeyRules()).containsExactly("수칙1", "수칙2");
        assertThat(res.getSourceLaws()).containsExactly("고용보험법");
    }

    @Test
    void getGuide_isCaseInsensitive() {
        when(crisisGuideRepository.findByCrisisType(CrisisType.UNEMPLOYMENT))
                .thenReturn(Optional.of(guideStub()));

        GuideResponse res = guideService.getGuide("unemployment");

        assertThat(res.getCrisisType()).isEqualTo("UNEMPLOYMENT");
    }

    @Test
    void getGuide_unknownType_throwsCrisisTypeNotFound() {
        assertThatThrownBy(() -> guideService.getGuide("NOT_A_CRISIS"))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.CRISIS_TYPE_NOT_FOUND);
    }

    @Test
    void getGuide_notFoundInRepository_throwsGuideNotFound() {
        when(crisisGuideRepository.findByCrisisType(CrisisType.ACCIDENT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> guideService.getGuide("ACCIDENT"))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.GUIDE_NOT_FOUND);
    }
}
