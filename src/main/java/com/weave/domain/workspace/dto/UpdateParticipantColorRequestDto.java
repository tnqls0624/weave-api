package com.weave.domain.workspace.dto;

import com.google.common.collect.ImmutableMap;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParticipantColorRequestDto {

  @NotNull(message = "Participant colors are required")
  private Map<String, String> participantColors; // key: userId, value: color code

  public Map<String, String> getParticipantColors() {
    return participantColors != null ? participantColors : ImmutableMap.of();
  }
}
