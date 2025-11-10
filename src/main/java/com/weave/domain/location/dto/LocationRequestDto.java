package com.weave.domain.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationRequestDto {

  @Schema(description = "위도", example = "37.5665")
  @NotNull(message = "위도는 필수입니다.")
  private Double latitude;

  @Schema(description = "경도", example = "126.9780")
  @NotNull(message = "경도는 필수입니다.")
  private Double longitude;
}
