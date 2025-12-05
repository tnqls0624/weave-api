package com.weave.domain.locationreminder.controller;

import com.weave.domain.locationreminder.dto.ArrivalNotificationDto;
import com.weave.domain.locationreminder.dto.LocationReminderDto;
import com.weave.domain.locationreminder.dto.SetLocationReminderDto;
import com.weave.domain.locationreminder.dto.ToggleLocationReminderDto;
import com.weave.domain.locationreminder.service.LocationReminderService;
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
@RequestMapping("/schedule/{scheduleId}/location-reminder")
@RequiredArgsConstructor
@Tag(name = "Location Reminder", description = "위치 기반 알림 API")
public class LocationReminderController {

  private final LocationReminderService locationReminderService;

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "위치 알림 설정 조회")
  @GetMapping
  public ApiResponse<LocationReminderDto> getReminder(@PathVariable String scheduleId) {
    return ApiResponse.ok(locationReminderService.getReminder(scheduleId));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "위치 알림 설정")
  @PostMapping
  public ApiResponse<LocationReminderDto> setReminder(
      @PathVariable String scheduleId,
      @Valid @RequestBody SetLocationReminderDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(locationReminderService.setReminder(scheduleId, dto, userDetails.getUsername()));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "위치 알림 활성화/비활성화")
  @PutMapping("/toggle")
  public ApiResponse<LocationReminderDto> toggleReminder(
      @PathVariable String scheduleId,
      @Valid @RequestBody ToggleLocationReminderDto dto
  ) {
    return ApiResponse.ok(locationReminderService.toggleReminder(scheduleId, dto));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "위치 알림 삭제")
  @DeleteMapping
  public ApiResponse<Void> deleteReminder(@PathVariable String scheduleId) {
    locationReminderService.deleteReminder(scheduleId);
    return ApiResponse.ok(null);
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "위치 도착 알림", description = "사용자가 설정된 위치에 도착했을 때 다른 참여자들에게 알림을 전송합니다.")
  @PostMapping("/arrival")
  public ApiResponse<Void> notifyArrival(
      @PathVariable String scheduleId,
      @Valid @RequestBody ArrivalNotificationDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    locationReminderService.notifyArrival(scheduleId, dto, userDetails.getUsername());
    return ApiResponse.ok(null);
  }
}
