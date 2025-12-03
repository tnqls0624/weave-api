package com.weave.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.weave.global.config.FlexibleDateDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequestScheduleDto {

  @Schema(example = "누나생일", description = "제목")
  @NotBlank
  private String title;

  @Schema(example = "누나 생일은 10월 6일", description = "설명")
  private String memo;

  @Schema(example = "2025-01-10T13:00:00", description = "시작 날짜")
  @JsonProperty("start_date")
  @JsonAlias({"startDate", "start_date"})
  @JsonDeserialize(using = FlexibleDateDeserializer.class)
  @NotNull
  private Date startDate;

  @Schema(example = "2025-01-11T13:00:00", description = "종료 날짜")
  @JsonProperty("end_date")
  @JsonAlias({"endDate", "end_date"})
  @JsonDeserialize(using = FlexibleDateDeserializer.class)
  @NotNull
  private Date endDate;

  @Schema(example = "NONE", description = "반복 타입")
  @JsonProperty("repeat_type")
  @JsonAlias({"repeatType", "repeat_type"})
  @NotNull
  private String repeatType;

  @Schema(example = "[\"66a61517670be7ef30b10244\", \"66a7ae7f25483684cf347cd9\"]", description = "참여자")
  @NotEmpty
  private List<String> participants;

  @Schema(example = "SOLAR", description = "양력, 음력")
  @JsonProperty("calendar_type")
  @JsonAlias({"calendarType", "calendar_type"})
  @NotNull
  private String calendarType;

  @Schema(example = "false", description = "종일 일정 여부")
  @JsonProperty("is_all_day")
  @JsonAlias({"isAllDay", "is_all_day"})
  private Boolean isAllDay;

  @Schema(example = "30", description = "알림 시간 (분 단위, null이면 알림 없음)")
  @JsonProperty("reminder_minutes")
  @JsonAlias({"reminderMinutes", "reminder_minutes"})
  private Integer reminderMinutes;

  @Schema(example = "false", description = "중요 일정 여부 (D-day 알림용)")
  @JsonProperty("is_important")
  @JsonAlias({"isImportant", "is_important"})
  private Boolean isImportant;
}
