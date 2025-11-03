package com.lovechedule.domain.auth.controller;

import com.lovechedule.domain.auth.dto.*;
import com.lovechedule.domain.auth.service.AuthService;
import com.lovechedule.domain.user.dto.UserResponseDto;
import com.lovechedule.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
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
    public ApiResponse<SocialLoginResponseDto> socialLogin(@Valid @RequestBody SocialLoginRequestDto dto) {
        return ApiResponse.ok(authService.socialLogin(dto));
    }

    // 개인 정보 수정
    @SecurityRequirement(name = "JWT")
    @Tag(name= "AUTH")
    @Operation(summary = "개인 정보 수정")
    @PutMapping("/update-user-info")
    public ApiResponse<UserResponseDto> updateUserInfo(@Valid @RequestBody UpdateUserRequestDto dto,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(authService.updateUserInfo(dto, userDetails.getUsername()));
    }

}
