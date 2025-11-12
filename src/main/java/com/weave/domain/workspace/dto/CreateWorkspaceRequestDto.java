package com.weave.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkspaceRequestDto {

  @Schema(example = "우리들의 다이어리", description = "워크스페이스 이름")
  @NotBlank
  private String title;

  @Schema(example = "2024-01-01", description = "사귀기 시작한 날짜")
  @NotBlank
  private String loveDay;
}
