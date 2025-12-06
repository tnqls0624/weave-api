package com.weave.domain.reaction.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResponseDto {

  private String emoji;
  private long count;
  private boolean isReactedByMe;
  private List<ReactedUserDto> users;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReactedUserDto {
    private String userId;
    private String userName;
    private String avatarUrl;
  }
}
