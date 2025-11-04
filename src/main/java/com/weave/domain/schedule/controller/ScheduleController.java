package com.weave.domain.schedule.controller;

import com.weave.domain.auth.dto.*;
import com.weave.domain.auth.service.AuthService;
import com.weave.domain.schedule.dto.CreateRequestScheduleDto;
import com.weave.domain.schedule.dto.ScheduleResponseDto;
import com.weave.domain.schedule.dto.UpdateRequestScheduleDto;
import com.weave.domain.schedule.service.ScheduleService;
import com.weave.domain.user.dto.UserResponseDto;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 스케줄 작성
    @SecurityRequirement(name = "JWT")
    @Tag(name = "Schedule")
    @Operation(summary = "스케줄 작성")
    @PostMapping("/")
    public ApiResponse<ScheduleResponseDto> create(@Valid @RequestBody CreateRequestScheduleDto dto, @RequestParam(value = "id", required = true) String id) {
        return ApiResponse.ok(scheduleService.create(dto, id));
    }

    // 스케줄 조회
    @SecurityRequirement(name = "JWT")
    @Tag(name= "Schedule")
    @Operation(summary = "스케줄 조회")
    @GetMapping("/{id}")
    public ApiResponse<ScheduleResponseDto> findById(@RequestParam(value = "id", required = true) String id) {
        return ApiResponse.ok(scheduleService.findById(id));
    }

    // 스케줄 수정
    @SecurityRequirement(name = "JWT")
    @Tag(name= "Schedule")
    @Operation(summary = "스케줄 수정")
    @PutMapping("/{id}")
    public ApiResponse<ScheduleResponseDto> update(@Valid @RequestBody UpdateRequestScheduleDto dto,
                                                   @RequestParam(value = "id", required = true) String id) {
        return ApiResponse.ok(scheduleService.update(dto, id));
    }

    // 스케줄 삭제
    @SecurityRequirement(name = "JWT")
    @Tag(name= "Schedule")
    @Operation(summary = "스케줄 삭제")
    @DeleteMapping("/{id}")
    public ApiResponse<ScheduleResponseDto> delete(@RequestParam(value = "id", required = true) String id) {
        return ApiResponse.ok(scheduleService.delete(id));
    }
}
