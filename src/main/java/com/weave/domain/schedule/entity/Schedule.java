package com.weave.domain.schedule.entity;

import jakarta.validation.constraints.NotBlank;
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
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

  @Id
  private ObjectId id;

  @Field("title")
  @NotBlank
  private String title;

  @Field("memo")
  private String memo;

  @Field("start_date")
  @NotBlank
  private String startDate;

  @Field("end_date")
  @NotBlank
  private String endDate;

  @Field("repeat_type")
  @Builder.Default
  private String repeatType = "none";

  @Field("participants")
  @Builder.Default
  private List<ObjectId> participants = List.of();

  @Field("calendar_type")
  @Builder.Default
  private String calendarType = "solar";

  @Field("workspace")
  private ObjectId workspace;

  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  private Date updatedAt;
}
