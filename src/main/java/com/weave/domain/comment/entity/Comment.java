package com.weave.domain.comment.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

  @Id
  private ObjectId id;

  @Field("schedule_id")
  @Indexed
  private ObjectId scheduleId;

  @Field("content")
  private String content;

  @Field("author_id")
  @Indexed
  private ObjectId authorId;

  // 답글 기능: 부모 댓글 ID (null이면 일반 댓글)
  @Field("parent_id")
  @Indexed
  private ObjectId parentId;

  // 멘션된 사용자 ID 목록
  @Field("mentions")
  @Builder.Default
  private List<ObjectId> mentions = new ArrayList<>();

  // 수정 여부
  @Field("is_edited")
  @Builder.Default
  private Boolean isEdited = false;

  @CreatedDate
  @Field("created_at")
  private Date createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private Date updatedAt;
}
