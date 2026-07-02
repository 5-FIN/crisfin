package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.domain.welfare.dto.WelfareBenefitResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WelfareFavoriteService {

    private final WelfareFavoriteRepository welfareFavoriteRepository;
    private final WelfareBenefitRepository welfareBenefitRepository;

    /**
     * 복지 제도를 즐겨찾기에 추가한다. 대상 제도가 존재하지 않으면 404를 던지고,
     * 이미 즐겨찾기되어 있으면 아무 것도 하지 않는다(멱등).
     */
    @Transactional
    public void addFavorite(Long userId, Long welfareBenefitId) {
        if (!welfareBenefitRepository.existsById(welfareBenefitId)) {
            throw new CrisfinException(ErrorCode.WELFARE_NOT_FOUND,
                    "요청한 복지 제도를 찾을 수 없습니다. id=" + welfareBenefitId);
        }
        if (welfareFavoriteRepository.existsByUserIdAndWelfareBenefitId(userId, welfareBenefitId)) {
            return;
        }
        welfareFavoriteRepository.save(WelfareFavorite.of(userId, welfareBenefitId));
    }

    /** 즐겨찾기를 해제한다. 즐겨찾기되어 있지 않아도 아무 것도 하지 않는다(멱등). */
    @Transactional
    public void removeFavorite(Long userId, Long welfareBenefitId) {
        welfareFavoriteRepository.deleteByUserIdAndWelfareBenefitId(userId, welfareBenefitId);
    }

    /**
     * 사용자의 즐겨찾기 목록을 최신순으로 반환한다. 복지 제도는 한 번에 조회해(N+1 방지)
     * 즐겨찾기 순서대로 정렬하며, 더 이상 존재하지 않는 제도는 건너뛴다.
     */
    @Transactional(readOnly = true)
    public List<WelfareBenefitResponse> listFavorites(Long userId) {
        List<WelfareFavorite> favorites =
                welfareFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Long> benefitIds = favorites.stream()
                .map(WelfareFavorite::getWelfareBenefitId)
                .collect(Collectors.toList());

        Map<Long, WelfareBenefit> benefitsById = welfareBenefitRepository.findAllById(benefitIds).stream()
                .collect(Collectors.toMap(WelfareBenefit::getId, Function.identity()));

        return benefitIds.stream()
                .map(benefitsById::get)
                .filter(benefit -> benefit != null)
                .map(WelfareBenefitResponse::from)
                .collect(Collectors.toList());
    }
}
