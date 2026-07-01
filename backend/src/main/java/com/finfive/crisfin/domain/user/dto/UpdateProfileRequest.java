package com.finfive.crisfin.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code PUT /api/v1/users/me} — updates the authenticated user's
 * editable profile fields. Region is optional (may be cleared by sending blanks);
 * nickname is required.
 */
@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
    private String nickname;

    /** 선택: 시도. 지역 기반 복지 추천에 사용. 공백이면 지역 미설정으로 처리. */
    @Size(max = 60)
    private String regionCtpv;

    /** 선택: 시군구. 지역 기반 복지 추천에 사용. */
    @Size(max = 60)
    private String regionSgg;
}
