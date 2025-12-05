package com.weave.domain.comment.entity;

import java.util.Date;
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

  @CreatedDate
  @Field("created_at")
  private Date createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private Date updatedAt;
}
