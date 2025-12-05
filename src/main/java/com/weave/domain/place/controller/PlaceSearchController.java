package com.weave.domain.place.controller;

import com.weave.domain.place.dto.PlaceSearchResponseDto;
import com.weave.domain.place.service.NaverPlaceSearchService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Place Search", description = "장소 검색 API")
@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceSearchController {

  private final NaverPlaceSearchService naverPlaceSearchService;

  @Operation(summary = "장소 검색", description = "네이버 지역 검색 API를 사용하여 장소를 검색합니다.")
  @GetMapping("/search")
  public ApiResponse<PlaceSearchResponseDto> searchPlaces(
      @RequestParam String query,
      @RequestParam(defaultValue = "5") int display
  ) {
    PlaceSearchResponseDto result = naverPlaceSearchService.searchPlaces(query, display);
    return ApiResponse.ok(result);
  }
}
