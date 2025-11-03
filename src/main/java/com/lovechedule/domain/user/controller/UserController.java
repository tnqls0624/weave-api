package com.lovechedule.domain.user.controller;

import com.lovechedule.domain.user.dto.UpdateNotificationRequestDto;
import com.lovechedule.domain.user.dto.UserResponseDto;
import com.lovechedule.domain.user.service.UserService;
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
@RequestMapping("/api/user")
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
    public ApiResponse<UserResponseDto> updateNotification(@Valid @RequestBody UpdateNotificationRequestDto dto,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(userService.updateNotification(dto, userDetails.getUsername()));
    }
}
