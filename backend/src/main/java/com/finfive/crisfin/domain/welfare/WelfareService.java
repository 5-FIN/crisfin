package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.domain.welfare.dto.WelfareBenefitResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelfareService {

    private final WelfareBenefitRepository welfareBenefitRepository;

    /**
     * Returns a paginated list of active welfare benefits, optionally filtered
     * by a crisis type tag.
     *
     * @param crisisType optional crisis type string (e.g. "UNEMPLOYMENT"); blank means no filter
     * @param pageable   pagination and sorting parameters
     * @return page of {@link WelfareBenefitResponse}
     */
    public Page<WelfareBenefitResponse> getWelfareBenefits(String crisisType, Pageable pageable) {
        Page<WelfareBenefit> page;

        if (StringUtils.hasText(crisisType)) {
            page = welfareBenefitRepository.findByIsActiveTrueAndCrisisTagsContaining(
                    crisisType.trim().toUpperCase(), pageable);
        } else {
            page = welfareBenefitRepository.findByIsActiveTrue(pageable);
        }

        return page.map(WelfareBenefitResponse::from);
    }

    /**
     * Returns a single welfare benefit by its internal database ID.
     *
     * @param id the benefit's primary key
     * @return {@link WelfareBenefitResponse}
     * @throws CrisfinException with {@link ErrorCode#WELFARE_API_UNAVAILABLE} if not found
     */
    public WelfareBenefitResponse getWelfareBenefit(Long id) {
        WelfareBenefit benefit = welfareBenefitRepository.findById(id)
                .orElseThrow(() -> new CrisfinException(ErrorCode.WELFARE_API_UNAVAILABLE,
                        "복지 혜택 정보를 찾을 수 없습니다. id=" + id));

        return WelfareBenefitResponse.from(benefit);
    }
}
