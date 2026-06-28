package com.finfive.crisfin.domain.analysis;

import com.finfive.crisfin.domain.analysis.dto.AnalysisRequest;
import com.finfive.crisfin.domain.analysis.dto.AnalysisResultResponse;
import com.finfive.crisfin.domain.user.User;
import com.finfive.crisfin.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    /**
     * Triggers an AI analysis for the provided crisis scenario.
     *
     * <p>If the request is authenticated the user's ID is extracted from the
     * {@link UserDetails} principal (cast to {@link User} which implements
     * {@code UserDetails}). Anonymous callers pass {@code null} as the user ID.</p>
     *
     * @param request     validated analysis request body
     * @param userDetails Spring Security principal, {@code null} for anonymous callers
     * @return {@link ApiResponse} wrapping the analysis result
     */
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> recommend(
            @Valid @RequestBody AnalysisRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        AnalysisResultResponse response = analysisService.recommend(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Fetches a previously saved analysis result by its primary key.
     *
     * @param id analysis result ID
     * @return {@link ApiResponse} wrapping the stored analysis result
     */
    @GetMapping("/results/{id}")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> getResult(@PathVariable Long id) {
        AnalysisResultResponse response = analysisService.getResult(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
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
}
