package com.weave.domain.auth.controller;

import com.weave.domain.auth.dto.LoginRequestDto;
import com.weave.domain.auth.dto.LoginResponseDto;
import com.weave.domain.auth.dto.SocialLoginRequestDto;
import com.weave.domain.auth.dto.SocialLoginResponseDto;
import com.weave.domain.auth.dto.UpdateUserRequestDto;
import com.weave.domain.auth.service.AuthService;
import com.weave.domain.user.dto.UserResponseDto;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  // 이메일 로그인
  @Tag(name = "AUTH")
  @Operation(summary = "이메일 로그인")
  @PostMapping("/login")
  public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
    return ApiResponse.ok(authService.login(dto));
  }

  // 소셜 로그인
  @Tag(name = "AUTH")
  @Operation(summary = "소셜 로그인")
  @PostMapping("/social-login")
  public ApiResponse<SocialLoginResponseDto> socialLogin(
      @Valid @RequestBody SocialLoginRequestDto dto) {
    return ApiResponse.ok(authService.socialLogin(dto));
  }

  // 개인 정보 수정
  @SecurityRequirement(name = "JWT")
  @Tag(name = "AUTH")
  @Operation(summary = "개인 정보 수정")
  @PutMapping("/update")
  public ApiResponse<UserResponseDto> update(@Valid @RequestBody UpdateUserRequestDto dto,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(authService.update(dto, userDetails.getUsername()));
  }

}
