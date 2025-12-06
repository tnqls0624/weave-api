package com.weave.domain.reaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDto {

  @NotBlank(message = "이모지는 필수입니다")
  @Pattern(regexp = "^(👍|❤️|🎉|👀|🙏|😢)$", message = "허용되지 않은 이모지입니다")
  private String emoji;
}
