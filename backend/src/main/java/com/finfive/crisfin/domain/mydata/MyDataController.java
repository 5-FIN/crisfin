package com.finfive.crisfin.domain.mydata;

import com.finfive.crisfin.domain.mydata.dto.MyDataFilterRequest;
import com.finfive.crisfin.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mydata")
@RequiredArgsConstructor
public class MyDataController {

    private final MyDataService myDataService;

    /**
     * GET /api/v1/mydata/mock?persona=OFFICE_WORKER
     * Returns the full mock MyData financial profile for the given persona.
     */
    @GetMapping("/mock")
    public ApiResponse<Map<String, Object>> getMockData(
            @RequestParam String persona) {
        Map<String, Object> data = myDataService.getMockData(persona);
        return ApiResponse.ok(data);
    }

    /**
     * POST /api/v1/mydata/filter
     * Returns only the selected fields from the mock MyData financial profile.
     */
    @PostMapping("/filter")
    public ApiResponse<Map<String, Object>> filterData(
            @Valid @RequestBody MyDataFilterRequest request) {
        Map<String, Object> data = myDataService.filterData(request);
        return ApiResponse.ok(data);
    }
}
