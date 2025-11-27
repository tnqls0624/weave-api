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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "schedules")
@CompoundIndex(name = "workspace_startdate_idx", def = "{'workspace': 1, 'start_date': 1}")
@CompoundIndex(name = "workspace_enddate_idx", def = "{'workspace': 1, 'end_date': 1}")
@CompoundIndex(name = "workspace_dates_idx", def = "{'workspace': 1, 'start_date': 1, 'end_date': 1}")
@CompoundIndex(name = "participants_startdate_idx", def = "{'participants': 1, 'start_date': 1}")
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
  @Indexed
  private Date startDate;

  @Field("end_date")
  @Indexed
  private Date endDate;

  @Field("repeat_type")
  @Builder.Default
  private String repeatType = "none";

  @Field("participants")
  @Indexed
  @Builder.Default
  private List<ObjectId> participants = List.of();

  @Field("calendar_type")
  @Builder.Default
  private String calendarType = "solar";

  @Field("workspace")
  @Indexed
  private ObjectId workspace;

  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  private Date updatedAt;
}