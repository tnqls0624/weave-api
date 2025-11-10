package com.weave.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDto {

  @JsonProperty("locdate")
  private String locdate;

  @JsonProperty("dateName")
  private String dateName;

  @JsonProperty("isHoliday")
  private String isHoliday;
}
