package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.analysis.dto.AnalysisRequest;
import com.finfive.crisfin.domain.analysis.dto.AnalysisResultResponse;
import com.finfive.crisfin.domain.analysis.dto.ReinferRequest;
import com.finfive.crisfin.domain.analysis.dto.ShareLinkResponse;
import com.finfive.crisfin.domain.payment.PaymentService;
import com.finfive.crisfin.domain.user.User;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the AI-powered financial crisis analysis domain.
 *
 * <ul>
 *   <li>{@code POST /api/v1/analysis/recommend} – Run a new analysis (anonymous or authenticated).</li>
 *   <li>{@code GET  /api/v1/analysis/results/{id}} – Retrieve a previously saved analysis result.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final PaymentService paymentService;

    /**
     * Triggers an AI analysis for the provided crisis scenario. This is a paid endpoint:
     * unauthenticated callers receive 401 and authenticated callers without an active
     * entitlement receive 402 ({@code PAYMENT_REQUIRED}).
     *
     * @param request     validated analysis request body
     * @param userDetails Spring Security principal
     * @return {@link ApiResponse} wrapping the analysis result
     */
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> recommend(
            @Valid @RequestBody AnalysisRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = requirePaidUser(userDetails);
        AnalysisResultResponse response = analysisService.recommend(request, userId);
        paymentService.consumeUse(userId); // 성공한 분석만 이용권 1회 소모
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Re-runs a personalised analysis from an existing result. Paid + authenticated, same as
     * {@code /recommend}.
     */
    @PostMapping("/{id}/reinfer")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> reinfer(
            @PathVariable Long id,
            @RequestBody(required = false) ReinferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = requirePaidUser(userDetails);
        ReinferRequest body = (request != null) ? request : new ReinferRequest();
        AnalysisResultResponse response = analysisService.reinfer(id, body, userId);
        paymentService.consumeUse(userId); // 성공한 재분석만 이용권 1회 소모
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Returns paginated analysis history for the authenticated user.
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<AnalysisResultResponse>>> history(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = extractUserId(userDetails);
        Page<AnalysisResultResponse> result = analysisService.history(
                userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Fetches a previously saved analysis result by its primary key.
     *
     * @param id analysis result ID
     * @return {@link ApiResponse} wrapping the stored analysis result
     */
    @GetMapping("/results/{id}")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> getResult(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 로그인 필수 + 소유권 검증: 본인 소유 결과만 조회 가능(IDOR 방지).
        Long userId = extractUserId(userDetails);
        if (userId == null) {
            throw new CrisfinException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        AnalysisResultResponse response = analysisService.getResult(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Creates (or returns the existing) public share link for an owned analysis result.
     * 로그인 필수 + 소유권 검증.
     *
     * @param id          analysis result ID
     * @param userDetails Spring Security principal
     * @return {@link ApiResponse} wrapping the {@link ShareLinkResponse}
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> share(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        if (userId == null) {
            throw new CrisfinException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String token = analysisService.createShareLink(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(ShareLinkResponse.of(token)));
    }

    /**
     * Revokes the public share link for an owned analysis result. 로그인 필수 + 소유권 검증.
     *
     * @param id          analysis result ID
     * @param userDetails Spring Security principal
     */
    @DeleteMapping("/{id}/share")
    public ResponseEntity<ApiResponse<Void>> revokeShare(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        if (userId == null) {
            throw new CrisfinException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        analysisService.revokeShareLink(id, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * Publicly fetches a shared analysis result by its unguessable token. No auth, no
     * ownership — access is granted solely by holding the token (read-only).
     *
     * @param token the public share token
     * @return {@link ApiResponse} wrapping the stored analysis result
     */
    @GetMapping("/shared/{token}")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> getShared(
            @PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getSharedResult(token)));
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    /**
     * Extracts the numeric user ID from the principal.
     *
     * <p>The {@link User} entity implements {@link UserDetails}, so a direct cast is safe
     * when authentication is present. Returns {@code null} for unauthenticated requests.</p>
     */
    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        if (userDetails instanceof User user) {
            return user.getId();
        }
        return null;
    }

    /**
     * Resolves the authenticated user and enforces the paywall.
     *
     * @throws CrisfinException 401 {@code UNAUTHORIZED} when anonymous,
     *                          402 {@code PAYMENT_REQUIRED} when no active entitlement
     */
    private Long requirePaidUser(UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        if (userId == null) {
            throw new CrisfinException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!paymentService.hasActiveEntitlement(userId)) {
            throw new CrisfinException(ErrorCode.PAYMENT_REQUIRED, "분석 이용권이 필요합니다.");
        }
        return userId;
    }
}
