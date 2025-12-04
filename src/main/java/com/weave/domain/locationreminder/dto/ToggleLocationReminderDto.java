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
public class ToggleLocationReminderDto {

  @NotNull(message = "활성화 여부를 입력해주세요")
  @JsonProperty("isEnabled")
  private Boolean isEnabled;
}
