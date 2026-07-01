package com.finfive.crisfin.domain.user;

import com.finfive.crisfin.domain.user.dto.AuthResponse;
import com.finfive.crisfin.domain.user.dto.LoginRequest;
import com.finfive.crisfin.domain.user.dto.SignupRequest;
import com.finfive.crisfin.domain.user.dto.UpdateProfileRequest;
import com.finfive.crisfin.domain.user.dto.UserResponse;
import com.finfive.crisfin.global.config.JwtProvider;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    // ------------------------------------------------------------------ //
    //  Signup
    // ------------------------------------------------------------------ //

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CrisfinException(ErrorCode.UNAUTHORIZED, "이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .regionCtpv(request.getRegionCtpv())
                .regionSgg(request.getRegionSgg())
                .role(UserRole.USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        return issueTokens(user);
    }

    // ------------------------------------------------------------------ //
    //  Login
    // ------------------------------------------------------------------ //

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 브루트포스 방어: 연속 실패가 임계치를 넘으면 잠금 기간 동안 차단
        if (loginAttemptService.isLocked(request.getEmail())) {
            throw new CrisfinException(ErrorCode.LOGIN_LOCKED,
                    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(request.getEmail());
            throw new CrisfinException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        loginAttemptService.reset(request.getEmail());
        user.recordLogin();

        // Rotate refresh token
        refreshTokenRepository.deleteByUserId(user.getId());
        return issueTokens(user);
    }

    // ------------------------------------------------------------------ //
    //  Token refresh
    // ------------------------------------------------------------------ //

    @Transactional
    public AuthResponse refresh(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new CrisfinException(ErrorCode.TOKEN_INVALID));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new CrisfinException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new CrisfinException(ErrorCode.UNAUTHORIZED));

        // Rotate: delete old, issue new pair
        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    // ------------------------------------------------------------------ //
    //  Logout
    // ------------------------------------------------------------------ //

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    // ------------------------------------------------------------------ //
    //  Profile update
    // ------------------------------------------------------------------ //

    /**
     * Updates the authenticated user's editable profile (nickname + region). Blank region
     * values clear the field (region 미설정). Returns the refreshed profile view.
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CrisfinException(ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        user.updateNickname(request.getNickname().trim());
        user.updateRegion(blankToNull(request.getRegionCtpv()), blankToNull(request.getRegionSgg()));
        return UserResponse.from(user);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ------------------------------------------------------------------ //
    //  UserDetailsService
    // ------------------------------------------------------------------ //

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRole());
        String refreshTokenValue = jwtProvider.generateRefreshToken(user.getEmail());

        // Persist refresh token with expiry derived from jjwt claim (we mirror the
        // configured duration here to avoid re-parsing the signed token).
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(604800)) // 7 days
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .build();
    }
}
