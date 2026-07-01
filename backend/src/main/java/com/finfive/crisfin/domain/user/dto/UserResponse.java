package com.finfive.crisfin.domain.user.dto;

import com.finfive.crisfin.domain.user.User;
import lombok.Builder;
import lombok.Getter;

/**
 * Read-only view of the authenticated user's profile, including region
 * (시도/시군구) used for region-based welfare recommendation.
 */
@Getter
@Builder
public class UserResponse {

    private final String email;
    private final String nickname;
    private final String role;
    private final String regionCtpv;
    private final String regionSgg;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .email(u.getEmail())
                .nickname(u.getNickname())
                .role(u.getRole().name())
                .regionCtpv(u.getRegionCtpv())
                .regionSgg(u.getRegionSgg())
                .build();
    }
}
