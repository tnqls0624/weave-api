package com.weave.domain.auth.controller;

import com.weave.domain.auth.dto.LoginRequestDto;
import com.weave.domain.auth.dto.LoginResponseDto;
import com.weave.domain.auth.dto.RefreshTokenRequestDto;
import com.weave.domain.auth.dto.SocialLoginRequestDto;
import com.weave.domain.auth.dto.SocialLoginResponseDto;
import com.weave.domain.auth.dto.TestTokenRequestDto;
import com.weave.domain.auth.dto.TestTokenResponseDto;
import com.weave.domain.auth.service.AuthService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
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

  // Refresh Token으로 Access Token 재발급
  @Tag(name = "AUTH")
  @Operation(summary = "Access Token 재발급")
  @PostMapping("/refresh")
  public ApiResponse<LoginResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto dto) {
    return ApiResponse.ok(authService.refreshAccessToken(dto));
  }

  // 로그아웃
  @Tag(name = "AUTH")
  @Operation(summary = "로그아웃")
  @PostMapping("/logout")
  public ApiResponse<Void> logout(Principal principal) {
    authService.logout(principal.getName());
    return ApiResponse.ok(null);
  }

  // 테스트용 1년짜리 토큰 생성
  @Tag(name = "AUTH")
  @Operation(summary = "테스트용 1년 유효 토큰 생성", description = "개발/테스트 환경에서 사용. 서버 재부팅 후에도 유효합니다. 기본 이메일: yuki0624@nate.com")
  @PostMapping("/test-token")
  public ApiResponse<TestTokenResponseDto> generateTestToken() {
    return ApiResponse.ok(authService.generateTestToken("yuki0624@nate.com"));
  }
}
