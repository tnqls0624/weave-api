package com.weave.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weave.domain.place.dto.PlaceSearchResponseDto;
import com.weave.domain.place.dto.PlaceSearchResponseDto.PlaceItem;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NaverPlaceSearchService {

  @Value("${naver.api.client-id}")
  private String clientId;

  @Value("${naver.api.client-secret}")
  private String clientSecret;

  private static final String NAVER_LOCAL_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  /**
   * 네이버 지역 검색 API를 사용하여 장소 검색
   * @param query 검색어
   * @param display 결과 개수 (최대 5)
   * @return 검색 결과
   */
  public PlaceSearchResponseDto searchPlaces(String query, int display) {
    try {
      String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
      String url = String.format("%s?query=%s&display=%d&sort=random",
          NAVER_LOCAL_SEARCH_URL, encodedQuery, Math.min(display, 5));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("X-Naver-Client-Id", clientId)
          .header("X-Naver-Client-Secret", clientSecret)
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error("네이버 API 호출 실패: status={}, body={}", response.statusCode(), response.body());
        return PlaceSearchResponseDto.builder()
            .items(List.of())
            .total(0)
            .build();
      }

      return parseResponse(response.body());

    } catch (Exception e) {
      log.error("장소 검색 중 오류 발생: query={}", query, e);
      return PlaceSearchResponseDto.builder()
          .items(List.of())
          .total(0)
          .build();
    }
  }

  private PlaceSearchResponseDto parseResponse(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode items = root.get("items");
      int total = root.get("total").asInt();

      List<PlaceItem> placeItems = new ArrayList<>();

      if (items != null && items.isArray()) {
        for (JsonNode item : items) {
          // 네이버 좌표는 KATEC 좌표계 (mapx, mapy)이므로 변환 필요
          String mapx = item.get("mapx").asText();
          String mapy = item.get("mapy").asText();

          // KATEC -> WGS84 변환
          double[] wgs84 = convertKatecToWgs84(mapx, mapy);

          PlaceItem placeItem = PlaceItem.builder()
              .title(removeHtmlTags(item.get("title").asText()))
              .address(item.get("address").asText())
              .roadAddress(item.has("roadAddress") ? item.get("roadAddress").asText() : "")
              .latitude(wgs84[0])
              .longitude(wgs84[1])
              .category(item.has("category") ? item.get("category").asText() : "")
              .telephone(item.has("telephone") ? item.get("telephone").asText() : "")
              .build();

          placeItems.add(placeItem);
        }
      }

      return PlaceSearchResponseDto.builder()
          .items(placeItems)
          .total(total)
          .build();

    } catch (Exception e) {
      log.error("응답 파싱 중 오류 발생", e);
      return PlaceSearchResponseDto.builder()
          .items(List.of())
          .total(0)
          .build();
    }
  }

  /**
   * 네이버 KATEC 좌표를 WGS84 좌표로 변환
   * 네이버 지역검색 API의 mapx, mapy는 KATEC 좌표계 사용
   */
  private double[] convertKatecToWgs84(String mapx, String mapy) {
    try {
      // 네이버 API의 좌표는 정수형 KATEC 좌표 (예: 1269508536, 375206054)
      // 실제로는 경도/위도 * 10000000 형태로 제공됨
      double longitude = Double.parseDouble(mapx) / 10000000.0;
      double latitude = Double.parseDouble(mapy) / 10000000.0;

      return new double[]{latitude, longitude};
    } catch (NumberFormatException e) {
      log.warn("좌표 변환 실패: mapx={}, mapy={}", mapx, mapy);
      return new double[]{0.0, 0.0};
    }
  }

  /**
   * HTML 태그 제거 (네이버 API 응답에 <b> 태그가 포함됨)
   */
  private String removeHtmlTags(String text) {
    if (text == null) return "";
    return text.replaceAll("<[^>]*>", "").trim();
  }
}
