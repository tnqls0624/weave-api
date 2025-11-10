package com.weave.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weave.domain.schedule.entity.Schedule;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponseDto {

  private String id;

  private String title;

  private String memo;

  @JsonProperty("start_date")
  private String startDate;

  @JsonProperty("end_date")
  private String endDate;

  @JsonProperty("repeat_type")
  private String repeatType;

  @JsonProperty("calendar_type")
  private String calendarType;

  private List<String> participants;

  private String workspace;

  public static ScheduleResponseDto from(Schedule schedule) {
    return ScheduleResponseDto.builder()
        .id(schedule.getId().toString())
        .title(schedule.getTitle())
        .memo(schedule.getMemo())
        .startDate(schedule.getStartDate())
        .endDate(schedule.getEndDate())
        .repeatType(schedule.getRepeatType())
        .calendarType(schedule.getCalendarType())
        .participants(schedule.getParticipants() != null
            ? schedule.getParticipants().stream()
            .map(ObjectId::toString)
            .collect(Collectors.toList())
            : List.of())
        .workspace(schedule.getWorkspace() != null
            ? schedule.getWorkspace().toString()
            : null)
        .build();
  }
}
