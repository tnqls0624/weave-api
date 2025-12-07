package com.weave.domain.schedule.controller;

import com.weave.domain.schedule.dto.CreateRequestScheduleDto;
import com.weave.domain.schedule.dto.ScheduleCountsDto;
import com.weave.domain.schedule.dto.ScheduleResponseDto;
import com.weave.domain.schedule.dto.UpdateRequestScheduleDto;
import com.weave.domain.schedule.service.ScheduleService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

  private final ScheduleService scheduleService;

  // 스케줄 작성
  @SecurityRequirement(name = "JWT")
  @Tag(name = "Schedule")
  @Operation(summary = "스케줄 작성")
  @PostMapping
  public ApiResponse<ScheduleResponseDto> create(
      @Valid @RequestBody CreateRequestScheduleDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(scheduleService.create(dto, userDetails.getUsername()));
  }

  // 스케줄 조회
  @SecurityRequirement(name = "JWT")
  @Tag(name = "Schedule")
  @Operation(summary = "스케줄 조회")
  @GetMapping("/{id}")
  public ApiResponse<ScheduleResponseDto> findById(@PathVariable String id) {
    return ApiResponse.ok(scheduleService.findById(id));
  }

  // 스케줄 수정
  @SecurityRequirement(name = "JWT")
  @Tag(name = "Schedule")
  @Operation(summary = "스케줄 수정")
  @PutMapping("/{id}")
  public ApiResponse<ScheduleResponseDto> update(@Valid @RequestBody UpdateRequestScheduleDto dto,
      @PathVariable String id) {
    return ApiResponse.ok(scheduleService.update(dto, id));
  }

  // 스케줄 삭제
  @SecurityRequirement(name = "JWT")
  @Tag(name = "Schedule")
  @Operation(summary = "스케줄 삭제")
  @DeleteMapping("/{id}")
  public ApiResponse<ScheduleResponseDto> delete(@PathVariable String id) {
    return ApiResponse.ok(scheduleService.delete(id));
  }

  // 스케줄 카운트 조회 (댓글 수, 사진 수)
  @SecurityRequirement(name = "JWT")
  @Tag(name = "Schedule")
  @Operation(summary = "스케줄 카운트 조회 (댓글 수, 사진 수)")
  @GetMapping("/{id}/counts")
  public ApiResponse<ScheduleCountsDto> getCounts(@PathVariable String id) {
    return ApiResponse.ok(scheduleService.getCounts(id));
  }
}
