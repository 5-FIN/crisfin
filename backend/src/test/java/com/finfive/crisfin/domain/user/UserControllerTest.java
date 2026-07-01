package com.finfive.crisfin.domain.user;

import com.finfive.crisfin.domain.user.dto.UserResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link UserController} — /me 프로필 조회를 컨트롤러 직접 호출로 검증.
 * (보안은 Spring 런타임에서 강제됨; 여기선 principal 처리 로직만 검증)
 */
class UserControllerTest {

    private final UserController controller = new UserController();

    private User user() {
        return User.builder()
                .id(1L)
                .email("u@test.com")
                .passwordHash("hash")
                .nickname("tester")
                .regionCtpv("서울특별시")
                .regionSgg("강남구")
                .build();
    }

    @Test
    void me_authenticated_returnsProfileWithRegion() {
        ResponseEntity<ApiResponse<UserResponse>> res = controller.me(user());

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();

        UserResponse data = res.getBody().getData();
        assertThat(data.getEmail()).isEqualTo("u@test.com");
        assertThat(data.getNickname()).isEqualTo("tester");
        assertThat(data.getRole()).isEqualTo("USER");
        assertThat(data.getRegionCtpv()).isEqualTo("서울특별시");
        assertThat(data.getRegionSgg()).isEqualTo("강남구");
    }

    @Test
    void me_anonymous_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.me(null))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
