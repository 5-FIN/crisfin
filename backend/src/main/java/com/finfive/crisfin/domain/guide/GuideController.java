package com.finfive.crisfin.domain.guide;

import com.finfive.crisfin.domain.guide.dto.GuideResponse;
import com.finfive.crisfin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guide")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    @GetMapping("/{crisisType}")
    public ResponseEntity<ApiResponse<GuideResponse>> getGuide(
            @PathVariable String crisisType) {
        GuideResponse response = guideService.getGuide(crisisType);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
