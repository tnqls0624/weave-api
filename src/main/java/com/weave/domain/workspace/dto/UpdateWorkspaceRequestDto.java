package com.weave.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkspaceRequestDto {

  @Schema(
      example = "2024-01-01",
      description = "사귀기 시작한 날짜"
  )
  private String loveDay;

  @Schema(example = "[\"68576234782d9c909f1b5ff2\", \"68576234782d9c909f1b5ff3\"]")
  private List<String> users;

  @Schema(
      example = "http://test.com/image",
      description = "썸네일 이미지"
  )
  private String thumbnailImage;
}
