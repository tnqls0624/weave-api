package com.weave.domain.schedulephoto.controller;

import com.weave.domain.schedulephoto.dto.SchedulePhotoDto;
import com.weave.domain.schedulephoto.service.SchedulePhotoService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/schedule/{scheduleId}/photos")
@RequiredArgsConstructor
@Tag(name = "Schedule Photos", description = "일정 사진 앨범 API")
public class SchedulePhotoController {

  private final SchedulePhotoService schedulePhotoService;

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "일정 사진 목록 조회")
  @GetMapping
  public ApiResponse<List<SchedulePhotoDto>> getPhotos(@PathVariable String scheduleId) {
    return ApiResponse.ok(schedulePhotoService.getPhotos(scheduleId));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "일정 사진 업로드")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<SchedulePhotoDto> uploadPhoto(
      @PathVariable String scheduleId,
      @RequestParam("image") MultipartFile file,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(schedulePhotoService.uploadPhoto(scheduleId, file, userDetails.getUsername()));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "일정 사진 삭제")
  @DeleteMapping("/{photoId}")
  public ApiResponse<Void> deletePhoto(
      @PathVariable String scheduleId,
      @PathVariable String photoId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    schedulePhotoService.deletePhoto(scheduleId, photoId, userDetails.getUsername());
    return ApiResponse.ok(null);
  }
}
