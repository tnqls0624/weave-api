package com.weave.domain.schedulephoto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePhotoDto {

  private String id;
  private String url;

  @JsonProperty("thumbnailUrl")
  private String thumbnailUrl;

  @JsonProperty("uploadedBy")
  private String uploadedBy;

  @JsonProperty("uploadedByName")
  private String uploadedByName;

  @JsonProperty("uploadedAt")
  private String uploadedAt;

  private String caption;
}
