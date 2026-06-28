package com.finfive.crisfin.domain.user;

import com.finfive.crisfin.domain.user.dto.AuthResponse;
import com.finfive.crisfin.domain.user.dto.LoginRequest;
import com.finfive.crisfin.domain.user.dto.RefreshRequest;
import com.finfive.crisfin.domain.user.dto.SignupRequest;
import com.finfive.crisfin.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints — all publicly accessible (no JWT required).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Register a new user account and receive an initial token pair.
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse authResponse = userService.signup(request);
        return ApiResponse.ok(authResponse, "회원가입이 완료되었습니다.");
    }

    /**
     * Authenticate with email + password; returns a new token pair.
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = userService.login(request);
        return ApiResponse.ok(authResponse, "로그인에 성공했습니다.");
    }

    /**
     * Exchange a valid refresh token for a new access + refresh token pair
     * (token rotation — old refresh token is invalidated).
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse authResponse = userService.refresh(request.getRefreshToken());
        return ApiResponse.ok(authResponse, "토큰이 갱신되었습니다.");
    }

    /**
     * Invalidate the supplied refresh token (server-side logout).
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        userService.logout(request.getRefreshToken());
        return ApiResponse.ok(null, "로그아웃 되었습니다.");
    }
}
