package com.weave.domain.location.controller;

import com.weave.domain.location.dto.LocationRequestDto;
import com.weave.domain.location.dto.LocationResponseDto;
import com.weave.domain.location.service.LocationService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace/{workspaceId}/locations")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;

  @SecurityRequirement(name = "JWT")
  @Tag(name = "LOCATION")
  @Operation(summary = "워크스페이스에 내 위치 전송", description = "현재 사용자의 위치를 워크스페이스에 저장합니다.")
  @PostMapping
  public ApiResponse<LocationResponseDto> saveLocation(
      @PathVariable("workspaceId") String workspaceId,
      @Valid @RequestBody LocationRequestDto dto,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(locationService.saveLocation(workspaceId, dto, userDetails.getUsername()));
  }

  @SecurityRequirement(name = "JWT")
  @Tag(name = "LOCATION")
  @Operation(summary = "워크스페이스 참여자들의 위치 조회", description = "워크스페이스 내 모든 참여자의 최신 위치를 조회합니다.")
  @GetMapping
  public ApiResponse<List<LocationResponseDto>> getLocations(
      @PathVariable("workspaceId") String workspaceId) {
    return ApiResponse.ok(locationService.getLocations(workspaceId));
  }
}
