package com.weave.domain.locationreminder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetLocationReminderDto {

  @NotNull(message = "위도를 입력해주세요")
  private Double latitude;

  @NotNull(message = "경도를 입력해주세요")
  private Double longitude;

  @Builder.Default
  private Integer radius = 300;

  private String address;

  @JsonProperty("placeName")
  private String placeName;
}
