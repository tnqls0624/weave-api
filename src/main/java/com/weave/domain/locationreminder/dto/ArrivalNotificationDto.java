package com.weave.domain.locationreminder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArrivalNotificationDto {

  @NotNull
  private Double latitude;

  @NotNull
  private Double longitude;
}
