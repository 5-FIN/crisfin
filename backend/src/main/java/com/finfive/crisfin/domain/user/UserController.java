package com.finfive.crisfin.domain.user;

import com.finfive.crisfin.domain.user.dto.UserResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the authenticated user's own profile.
 *
 * <ul>
 *   <li>{@code GET /api/v1/users/me} – Return the current user's profile (지역 포함).</li>
 * </ul>
 *
 * <p>{@code /api/v1/users/**} is not listed in {@code SecurityConfig} permitAll, so it
 * falls under {@code anyRequest().authenticated()} and requires a valid JWT.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    /**
     * Returns the authenticated user's profile.
     *
     * <p>The {@link User} entity implements {@link UserDetails}, so the principal can be
     * cast directly when authentication is present.</p>
     *
     * @param userDetails Spring Security principal
     * @return {@link ApiResponse} wrapping the user's profile
     * @throws CrisfinException 401 {@code UNAUTHORIZED} when the request is anonymous
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails instanceof User user) {
            return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
        }
        throw new CrisfinException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
