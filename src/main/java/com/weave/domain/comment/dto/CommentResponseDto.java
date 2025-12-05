package com.weave.domain.comment.dto;

import com.weave.domain.comment.entity.Comment;
import java.util.Date;
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
  private Date createdAt;
  private Date updatedAt;

  public static CommentResponseDto from(Comment comment) {
    return CommentResponseDto.builder()
        .id(comment.getId().toHexString())
        .scheduleId(comment.getScheduleId().toHexString())
        .content(comment.getContent())
        .authorId(comment.getAuthorId().toHexString())
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
