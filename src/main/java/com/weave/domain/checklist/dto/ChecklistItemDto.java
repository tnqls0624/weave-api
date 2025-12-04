package com.weave.domain.checklist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemDto {

  private String id;
  private String content;

  @JsonProperty("isCompleted")
  private Boolean isCompleted;

  @JsonProperty("completedBy")
  private String completedBy;

  @JsonProperty("completedAt")
  private String completedAt;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdAt")
  private String createdAt;
}
