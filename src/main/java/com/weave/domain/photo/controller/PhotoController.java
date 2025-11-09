package com.weave.domain.photo.controller;

import com.weave.domain.photo.dto.PhotoUploadResponseDto;
import com.weave.domain.photo.service.PhotoService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/photo")
@RequiredArgsConstructor
public class PhotoController {

  private final PhotoService photoService;

  @SecurityRequirement(name = "JWT")
  @Tag(name = "PHOTO")
  @Operation(summary = "이미지 업로드", description = "이미지를 업로드하고, 이미지의 공개 URL을 반환합니다.")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<PhotoUploadResponseDto> upload(@RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(photoService.upload(file));
  }
}
