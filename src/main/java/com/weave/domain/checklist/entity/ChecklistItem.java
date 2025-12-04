package com.weave.domain.checklist.entity;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "checklist_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

  @Id
  private ObjectId id;

  @Field("schedule_id")
  @Indexed
  private ObjectId scheduleId;

  @Field("content")
  private String content;

  @Field("is_completed")
  @Builder.Default
  private Boolean isCompleted = false;

  @Field("completed_by")
  private ObjectId completedBy;

  @Field("completed_at")
  private Date completedAt;

  @Field("created_by")
  private ObjectId createdBy;

  @CreatedDate
  @Field("created_at")
  private Date createdAt;
}
