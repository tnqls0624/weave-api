package com.weave.domain.schedulephoto.entity;

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

@Document(collection = "schedule_photos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePhoto {

  @Id
  private ObjectId id;

  @Field("schedule_id")
  @Indexed
  private ObjectId scheduleId;

  @Field("url")
  private String url;

  @Field("thumbnail_url")
  private String thumbnailUrl;

  @Field("s3_key")
  private String s3Key;

  @Field("uploaded_by")
  private ObjectId uploadedBy;

  @Field("caption")
  private String caption;

  @CreatedDate
  @Field("uploaded_at")
  private Date uploadedAt;
}
