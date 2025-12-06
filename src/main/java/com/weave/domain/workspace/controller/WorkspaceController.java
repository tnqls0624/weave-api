package com.weave.domain.workspace.controller;

import com.weave.domain.auth.service.AuthService;
import com.weave.domain.schedulephoto.dto.GalleryPhotoDto;
import com.weave.domain.schedulephoto.service.SchedulePhotoService;
import com.weave.domain.workspace.dto.CreateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.UpdateParticipantColorRequestDto;
import com.weave.domain.workspace.dto.UpdateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.WorkspaceResponseDto;
import com.weave.domain.workspace.dto.WorkspaceScheduleResponseDto;
import com.weave.domain.workspace.service.WorkspaceService;
import com.weave.global.dto.ApiResponse;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

  private final WorkspaceService workspaceService;
  private final AuthService authService;
  private final SchedulePhotoService schedulePhotoService;

  // 워크스페이스 생성
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 생성")
  @PostMapping
  public ApiResponse<WorkspaceResponseDto> create(@Valid @RequestBody CreateWorkspaceRequestDto dto,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(workspaceService.create(dto, userDetails.getUsername()));
  }

  // 워크스페이스 수정
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 수정")
  @PutMapping("/{id}")
  public ApiResponse<WorkspaceResponseDto> update(@Valid @RequestBody UpdateWorkspaceRequestDto dto,
      @PathVariable("id") String id) {
    return ApiResponse.ok(workspaceService.update(dto, id));
  }

  // 워크스페이스 전부 찾기
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "내가 소속 된 워크스페이스 전부 찾기")
  @GetMapping
  public ApiResponse<WorkspaceResponseDto[]> find(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(workspaceService.find(userDetails.getUsername()));
  }

  // 워크스페이스 ID로 찾기
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 찾기")
  @GetMapping("/{id}")
  public ApiResponse<WorkspaceResponseDto> findById(@PathVariable("id") String id) {
    return ApiResponse.ok(workspaceService.findById(id));
  }

  // 워크스페이스 스케줄 조회
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스의 스케줄 찾기 (공휴일 포함)")
  @GetMapping("/{id}/schedule/")
  public ApiResponse<WorkspaceScheduleResponseDto> findWorkspaceSchedule(
      @PathVariable("id") String id,
      @RequestParam(value = "year", required = false) String year,
      @RequestParam(value = "month", required = false) String month,
      @RequestParam(value = "week", required = false) String week,
      @RequestParam(value = "day", required = false) String day) {
    return ApiResponse.ok(workspaceService.findWorkspaceSchedule(id, year, month, week, day));
  }

  // 참여자별 색상 지정
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 참여자별 스케줄 태그 색상 설정",
      description = "각 참여자의 스케줄에 표시될 태그 색상을 지정합니다. userId를 key로, 색상 코드(#RRGGBB)를 value로 전달합니다.")
  @PutMapping("/{id}/participant-colors")
  public ApiResponse<WorkspaceResponseDto> updateParticipantColors(
      @PathVariable("id") String id,
      @Valid @RequestBody UpdateParticipantColorRequestDto dto) {
    return ApiResponse.ok(workspaceService.updateParticipantColors(id, dto.getParticipantColors()));
  }

  // 워크스페이스 이번년도 피드 스케줄 조회
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스의 피드 스케줄 찾기)")
  @GetMapping("/{id}/schedule/feed")
  public ApiResponse<WorkspaceScheduleResponseDto> findWorkspaceScheduleFeed(
      @PathVariable("id") String id, @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(workspaceService.findWorkspaceScheduleFeed(id, userDetails));
  }

  // 초대코드로 워크스페이스 참여
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "초대코드로 워크스페이스 참여", description = "다른 사용자의 초대코드를 입력하여 해당 사용자의 워크스페이스에 참여합니다.")
  @PostMapping("/join/{inviteCode}")
  public ApiResponse<WorkspaceResponseDto> joinByInviteCode(
      @PathVariable("inviteCode") String inviteCode,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ApiResponse.ok(workspaceService.joinByInviteCode(inviteCode, userDetails.getUsername()));
  }

  // 워크스페이스 나가기
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 나가기", description = "현재 워크스페이스에서 나갑니다. 마스터는 나갈 수 없습니다.")
  @PostMapping("/{id}/leave")
  public ApiResponse<Void> leave(
      @PathVariable("id") String id,
      @AuthenticationPrincipal UserDetails userDetails) {
    workspaceService.leave(id, userDetails.getUsername());
    return ApiResponse.ok(null);
  }

  // 워크스페이스 삭제
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 삭제", description = "워크스페이스를 삭제합니다. 마스터만 삭제할 수 있습니다.")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @PathVariable("id") String id,
      @AuthenticationPrincipal UserDetails userDetails) {
    workspaceService.delete(id, userDetails.getUsername());
    return ApiResponse.ok(null);
  }

  // 워크스페이스 멤버 추방
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 멤버 추방", description = "워크스페이스에서 멤버를 추방합니다. 마스터만 추방할 수 있습니다.")
  @PostMapping("/{id}/kick/{userId}")
  public ApiResponse<Void> kickMember(
      @PathVariable("id") String id,
      @PathVariable("userId") String userId,
      @AuthenticationPrincipal UserDetails userDetails) {
    workspaceService.kickMember(id, userId, userDetails.getUsername());
    return ApiResponse.ok(null);
  }

  // 워크스페이스 갤러리 조회
  @SecurityRequirement(name = "JWT")
  @Tag(name = "WORKSPACE")
  @Operation(summary = "워크스페이스 갤러리 조회", description = "워크스페이스의 모든 스케줄 사진을 조회합니다.")
  @GetMapping("/{id}/gallery")
  public ApiResponse<List<GalleryPhotoDto>> getGallery(@PathVariable("id") String id) {
    return ApiResponse.ok(schedulePhotoService.getGalleryPhotos(id));
  }

}
