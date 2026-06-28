package com.finfive.crisfin.domain.guide;

import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.domain.guide.dto.GuideResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuideService {

    private final CrisisGuideRepository crisisGuideRepository;

    @Transactional(readOnly = true)
    public GuideResponse getGuide(String crisisTypeStr) {
        CrisisType crisisType = parseCrisisType(crisisTypeStr);

        CrisisGuide guide = crisisGuideRepository
                .findByCrisisType(crisisType)
                .orElseThrow(() -> new CrisfinException(ErrorCode.GUIDE_NOT_FOUND));

        return GuideResponse.builder()
                .crisisType(guide.getCrisisType().name())
                .title(guide.getTitle())
                .coachingPrompt(guide.getCoachingPrompt())
                .keyRules(guide.getKeyRules())
                .sourceLaws(guide.getSourceLaws())
                .build();
    }

    private CrisisType parseCrisisType(String crisisTypeStr) {
        try {
            return CrisisType.valueOf(crisisTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CrisfinException(ErrorCode.CRISIS_TYPE_NOT_FOUND);
        }
    }
}
