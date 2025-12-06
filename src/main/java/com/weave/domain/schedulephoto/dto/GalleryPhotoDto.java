package com.weave.domain.schedulephoto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryPhotoDto {

  private String id;
  private String url;

  @JsonProperty("thumbnailUrl")
  private String thumbnailUrl;

  @JsonProperty("scheduleId")
  private String scheduleId;

  @JsonProperty("scheduleTitle")
  private String scheduleTitle;

  @JsonProperty("scheduleDate")
  private Date scheduleDate;

  @JsonProperty("uploadedBy")
  private String uploadedBy;

  @JsonProperty("uploadedByName")
  private String uploadedByName;

  @JsonProperty("uploadedAt")
  private Date uploadedAt;

  private String caption;
}
