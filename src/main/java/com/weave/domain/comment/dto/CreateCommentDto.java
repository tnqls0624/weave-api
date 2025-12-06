package com.weave.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDto {

  @NotBlank(message = "댓글 내용은 필수입니다")
  private String content;

  // 답글인 경우 부모 댓글 ID
  private String parentId;

  // 멘션된 사용자 ID 목록
  private List<String> mentions;
}
