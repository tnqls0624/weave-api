package com.weave.domain.comment.dto;

import com.weave.domain.comment.entity.Comment;
import com.weave.domain.reaction.dto.ReactionResponseDto;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {

  private String id;
  private String scheduleId;
  private String content;
  private String authorId;
  private String authorName;
  private String authorAvatarUrl;
  private String parentId;
  private List<MentionedUserDto> mentions;
  private Boolean isEdited;
  private List<ReactionResponseDto> reactions;
  private List<CommentResponseDto> replies;  // 답글 목록
  private Date createdAt;
  private Date updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MentionedUserDto {
    private String userId;
    private String userName;
  }

  public static CommentResponseDto from(Comment comment) {
    return CommentResponseDto.builder()
        .id(comment.getId().toHexString())
        .scheduleId(comment.getScheduleId().toHexString())
        .content(comment.getContent())
        .authorId(comment.getAuthorId().toHexString())
        .parentId(comment.getParentId() != null ? comment.getParentId().toHexString() : null)
        .mentions(new ArrayList<>())
        .isEdited(comment.getIsEdited() != null ? comment.getIsEdited() : false)
        .reactions(new ArrayList<>())
        .replies(new ArrayList<>())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }

  public static CommentResponseDto from(Comment comment, String authorName, String authorAvatarUrl) {
    CommentResponseDto dto = from(comment);
    dto.setAuthorName(authorName);
    dto.setAuthorAvatarUrl(authorAvatarUrl);
    return dto;
  }
}
