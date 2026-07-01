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
     * Returns a paginated list of active welfare benefits, optionally filtered by
     * region (시도/시군구) and/or crisis type tag. Any blank filter is treated as
     * absent (no constraint on that column).
     *
     * @param ctpvNm     optional 시도 filter (e.g. "서울특별시"); blank means no filter
     * @param sggNm      optional 시군구 filter (e.g. "종로구"); blank means no filter
     * @param crisisType optional crisis type string (e.g. "UNEMPLOYMENT"); blank means no filter
     * @param pageable   pagination and sorting parameters
     * @return page of {@link WelfareBenefitResponse}
     */
    public Page<WelfareBenefitResponse> getWelfareBenefits(
            String ctpvNm, String sggNm, String crisisType, Pageable pageable) {

        String ctpv = StringUtils.hasText(ctpvNm) ? ctpvNm.trim() : null;
        String sgg = StringUtils.hasText(sggNm) ? sggNm.trim() : null;
        String tag = StringUtils.hasText(crisisType) ? crisisType.trim().toUpperCase() : null;

        return welfareBenefitRepository.search(ctpv, sgg, tag, pageable)
                .map(WelfareBenefitResponse::from);
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
