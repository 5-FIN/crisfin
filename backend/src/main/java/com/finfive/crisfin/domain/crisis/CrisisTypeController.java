package com.finfive.crisfin.domain.crisis;

import com.finfive.crisfin.domain.crisis.dto.CrisisTypeResponse;
import com.finfive.crisfin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Public endpoint — no authentication required.
 * Returns all supported crisis types with their metadata.
 */
@RestController
@RequestMapping("/api/v1/crisis")
@RequiredArgsConstructor
public class CrisisTypeController {

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<CrisisTypeResponse>>> getAllCrisisTypes() {
        List<CrisisTypeResponse> types = Arrays.stream(CrisisType.values())
                .map(CrisisTypeResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(types));
    }
}
