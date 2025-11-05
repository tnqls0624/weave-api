package com.weave.domain.user.controller;

import com.weave.domain.auth.dto.UpdateUserRequestDto;
import com.weave.domain.user.dto.UpdateNotificationRequestDto;
import com.weave.domain.user.dto.UserResponseDto;
import com.weave.domain.user.service.UserService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  // 초대코드 확인
  @Tag(name = "USER")
  @Operation(summary = "초대코드 확인")
  @GetMapping("/invite/{code}")
  public ApiResponse<Boolean> confirmInviteCode(@PathVariable("code") String code) {
    return ApiResponse.ok(userService.existsByInviteCode(code));
  }

  // 알림설정 업데이트
  @SecurityRequirement(name = "JWT")
  @Tag(name = "USER")
  @Operation(summary = "알림설정 업데이트")
  @PatchMapping("/notifications")
  public ApiResponse<UserResponseDto> updateNotification(
      @Valid @RequestBody UpdateNotificationRequestDto dto,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(userService.updateNotification(dto, userDetails.getUsername()));
  }

  // 개인 정보 조회
  @SecurityRequirement(name = "JWT")
  @Tag(name = "USER")
  @Operation(summary = "개인 정보 조회")
  @GetMapping("/me")
  public ApiResponse<UserResponseDto> findByEmail(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(userService.findByEmail(userDetails.getUsername()));
  }

  // 개인 정보 수정
  @SecurityRequirement(name = "JWT")
  @Tag(name = "USER")
  @Operation(summary = "개인 정보 수정")
  @PutMapping("/me")
  public ApiResponse<UserResponseDto> update(@Valid @RequestBody UpdateUserRequestDto dto,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(userService.update(dto, userDetails.getUsername()));
  }
}
