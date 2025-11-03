package com.weave.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9!@#$%^&*()_+=\\-]{6,20}$",
            message = "비밀번호는 영어, 숫자, 특수문자를 포함하여 6~20자 이내로 입력해야 합니다."
    )
    private String password;
}