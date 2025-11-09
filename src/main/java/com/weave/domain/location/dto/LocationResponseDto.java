package com.weave.domain.location.dto;

import com.weave.domain.location.entity.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationResponseDto {

  @Schema(description = "위치 ID")
  private String id;

  @Schema(description = "워크스페이스 ID")
  private String workspaceId;

  @Schema(description = "사용자 ID")
  private String userId;

  @Schema(description = "사용자 이름")
  private String userName;

  @Schema(description = "위도")
  private Double latitude;

  @Schema(description = "경도")
  private Double longitude;

  @Schema(description = "위치 업데이트 시간")
  private Date timestamp;

  public static LocationResponseDto from(Location location, String userName) {
    return LocationResponseDto.builder()
        .id(location.getId().toString())
        .workspaceId(location.getWorkspaceId().toString())
        .userId(location.getUserId().toString())
        .userName(userName)
        .latitude(location.getLatitude())
        .longitude(location.getLongitude())
        .timestamp(location.getTimestamp())
        .build();
  }
}
