package com.weave.domain.locationreminder.entity;

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

@Document(collection = "location_reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationReminder {

  @Id
  private ObjectId id;

  @Field("schedule_id")
  @Indexed(unique = true)
  private ObjectId scheduleId;

  @Field("latitude")
  private Double latitude;

  @Field("longitude")
  private Double longitude;

  @Field("radius")
  @Builder.Default
  private Integer radius = 300; // 미터 단위

  @Field("address")
  private String address;

  @Field("place_name")
  private String placeName;

  @Field("is_enabled")
  @Builder.Default
  private Boolean isEnabled = true;

  @Field("triggered_at")
  private Date triggeredAt;

  @Field("created_by")
  private ObjectId createdBy;

  @CreatedDate
  @Field("created_at")
  private Date createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private Date updatedAt;
}
