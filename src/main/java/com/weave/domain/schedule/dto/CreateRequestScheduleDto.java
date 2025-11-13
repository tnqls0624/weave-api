package com.weave.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRequestScheduleDto {

  @Schema(description = "워크스페이스 ID")
  @NotNull
  private String workspace;

  @Schema(example = "누나생일", description = "제목")
  @NotBlank
  private String title;

  @Schema(example = "누나 생일은 10월 6일", description = "설명")
  private String memo;

  @Schema(example = "2025-01-10T13:00:00", description = "시작 날짜")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
  @NotNull
  private Date startDate;

  @Schema(example = "2025-01-11T13:00:00", description = "종료 날짜")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
  @NotNull
  private Date endDate;

  @Schema(example = "none", description = "반복 타입")
  @NotNull
  private String repeatType;

  @Schema(example = "[\"66a61517670be7ef30b10244\", \"66a7ae7f25483684cf347cd9\"]", description = "참여자")
  @NotEmpty
  private List<String> participants;

  @Schema(example = "solar", description = "양력, 음력")
  @NotNull
  private String calendarType;
}
