package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.domain.welfare.dto.WelfareBenefitResponse;
import com.finfive.crisfin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/welfare")
@RequiredArgsConstructor
public class WelfareController {

    private final WelfareService welfareService;

    /**
     * GET /api/v1/welfare/benefits
     *
     * <p>Returns a paginated list of active welfare benefits. Pass {@code ctpvNm}
     * (시도) and/or {@code sggNm} (시군구) to narrow results by region, and/or
     * {@code crisisType} (e.g. {@code UNEMPLOYMENT}, {@code HOSPITALIZATION}) to
     * narrow by crisis tag stored in the JSONB column. All filters are optional
     * and combine (AND).
     *
     * @param ctpvNm     optional 시도 filter
     * @param sggNm      optional 시군구 filter
     * @param crisisType optional crisis type filter
     * @param keyword    optional 서비스명/요약 키워드 검색(ILIKE)
     * @param sort       {@code name}(가나다) 또는 {@code recent}(최신, 기본)
     * @return wrapped page of welfare benefit summaries
     */
    @GetMapping("/benefits")
    public ApiResponse<Page<WelfareBenefitResponse>> getWelfareBenefits(
            @RequestParam(required = false) String ctpvNm,
            @RequestParam(required = false) String sggNm,
            @RequestParam(required = false) String crisisType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 정렬은 네이티브 쿼리의 ORDER BY(:sortBy)가 처리한다(Pageable Sort는 네이티브 미적용).
        Pageable pageable = PageRequest.of(page, size);
        Page<WelfareBenefitResponse> result =
                welfareService.getWelfareBenefits(ctpvNm, sggNm, crisisType, keyword, sort, pageable);
        return ApiResponse.ok(result);
    }

    /**
     * GET /api/v1/welfare/benefits/{id}
     *
     * <p>Returns a single welfare benefit by its database ID.
     *
     * @param id the benefit's primary key
     * @return wrapped welfare benefit detail
     */
    @GetMapping("/benefits/{id}")
    public ApiResponse<WelfareBenefitResponse> getWelfareBenefit(@PathVariable Long id) {
        WelfareBenefitResponse response = welfareService.getWelfareBenefit(id);
        return ApiResponse.ok(response);
    }
}
