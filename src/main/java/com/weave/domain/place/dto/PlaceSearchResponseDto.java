package com.weave.domain.place.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResponseDto {
  private List<PlaceItem> items;
  private int total;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlaceItem {
    private String title;        // 장소명 (HTML 태그 포함 가능)
    private String address;      // 지번 주소
    private String roadAddress;  // 도로명 주소
    private double latitude;     // 위도
    private double longitude;    // 경도
    private String category;     // 카테고리
    private String telephone;    // 전화번호
  }
}
