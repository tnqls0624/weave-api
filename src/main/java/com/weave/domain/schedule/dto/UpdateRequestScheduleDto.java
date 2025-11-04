package com.weave.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weave.domain.schedule.entity.Schedule.CalendarType;
import com.weave.domain.schedule.entity.Schedule.RepeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRequestScheduleDto {

  @Schema(example = "누나생일", description = "제목")
  @NotBlank
  private String title;

  @Schema(example = "누나 생일은 10월 6일", description = "설명")
  private String memo;

  @Schema(example = "2025-01-10 13:00:00", description = "시작 날짜")
  @JsonProperty("start_date")
  @NotBlank
  private String startDate;

  @Schema(example = "2025-01-11 13:00:00", description = "종료 날짜")
  @JsonProperty("end_date")
  @NotBlank
  private String endDate;

  @Schema(example = "NONE", description = "반복 타입")
  @JsonProperty("repeat_type")
  @NotNull
  private RepeatType repeatType;

  @Schema(example = "[\"66a61517670be7ef30b10244\", \"66a7ae7f25483684cf347cd9\"]", description = "참여자")
  @NotEmpty
  private List<String> participants;

//    @Schema(example = "false", description = "기념일 여부")
//    @JsonProperty("is_anniversary")
//    @NotNull
//    private Boolean isAnniversary;

  @Schema(example = "SOLAR", description = "양력, 음력")
  @JsonProperty("calendar_type")
  @NotNull
  private CalendarType calendarType;
}
