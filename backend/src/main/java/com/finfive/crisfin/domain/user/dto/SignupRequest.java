package com.finfive.crisfin.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    /** 선택 입력: 사용자 지역(시도). 지역 기반 복지 추천에 사용. */
    private String regionCtpv;

    /** 선택 입력: 사용자 지역(시군구). 지역 기반 복지 추천에 사용. */
    private String regionSgg;
}
