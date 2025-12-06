package com.weave.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkspaceRequestDto {

  @Schema(
      example = "우리들의 다이어리",
      description = "워크스페이스 이름"
  )
  private String title;

  @Schema(example = "[\"68576234782d9c909f1b5ff2\", \"68576234782d9c909f1b5ff3\"]")
  private List<String> users;
}
