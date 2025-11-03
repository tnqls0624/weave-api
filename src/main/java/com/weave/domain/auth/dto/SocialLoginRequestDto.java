package com.weave.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialLoginRequestDto {

    @NotBlank(message = "login_type 필수입니다.")
    @Pattern(
            regexp = "KAKAO|GOOGLE|NAVER|FACEBOOK|APPLE",
            message = "허용되지 않은 로그인 타입입니다. (KAKAO, GOOGLE, NAVER, FACEBOOK, APPLE 중 하나여야 합니다.)"
    )
    private String loginType;

    @NotBlank(message = "access_token은 필수입니다.")
    private String accessToken;
}
