package com.finfive.crisfin.domain.welfare;

import com.finfive.crisfin.domain.user.User;
import com.finfive.crisfin.domain.welfare.dto.WelfareBenefitResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 복지 즐겨찾기 엔드포인트. {@code /api/v1/welfare/**}는 permitAll이므로 각 핸들러에서
 * 직접 principal로부터 사용자를 확인하고, 익명 요청이면 401을 던진다.
 *
 * <ul>
 *   <li>{@code POST   /api/v1/welfare/favorites/{welfareId}} – 즐겨찾기 추가(멱등).</li>
 *   <li>{@code DELETE /api/v1/welfare/favorites/{welfareId}} – 즐겨찾기 해제(멱등).</li>
 *   <li>{@code GET    /api/v1/welfare/favorites}             – 내 즐겨찾기 목록(최신순).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/welfare/favorites")
@RequiredArgsConstructor
public class WelfareFavoriteController {

    private final WelfareFavoriteService welfareFavoriteService;

    @PostMapping("/{welfareId}")
    public ApiResponse<Void> addFavorite(
            @PathVariable Long welfareId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = requireUserId(userDetails);
        welfareFavoriteService.addFavorite(userId, welfareId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{welfareId}")
    public ApiResponse<Void> removeFavorite(
            @PathVariable Long welfareId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = requireUserId(userDetails);
        welfareFavoriteService.removeFavorite(userId, welfareId);
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<List<WelfareBenefitResponse>> listFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = requireUserId(userDetails);
        return ApiResponse.ok(welfareFavoriteService.listFavorites(userId));
    }

    private Long requireUserId(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getId();
        }
        throw new CrisfinException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
