package com.weave.domain.locationreminder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationReminderDto {

  private String id;

  @JsonProperty("scheduleId")
  private String scheduleId;

  private Double latitude;
  private Double longitude;
  private Integer radius;
  private String address;

  @JsonProperty("placeName")
  private String placeName;

  @JsonProperty("isEnabled")
  private Boolean isEnabled;

  @JsonProperty("triggeredAt")
  private String triggeredAt;
}
