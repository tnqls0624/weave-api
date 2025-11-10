package com.weave.domain.schedule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.weave.domain.schedule.dto.HolidayDto;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class HolidayService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  @Value("${holiday.api.service-key}")
  private String serviceKey;

  @Value("${holiday.api.base-url}")
  private String baseUrl;

  public HolidayService(RedisTemplate<String, Object> redisTemplate, WebClient webClient,
      ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.webClient = webClient;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public void onStartup() {
    log.info("HolidayService has been initialized.");
    try {
      int currentYear = LocalDate.now().getYear();
      List<Integer> yearsToFetch = ImmutableList.of(currentYear - 1, currentYear, currentYear + 1);

      yearsToFetch.forEach(this::fetchAndCacheYearData);

      cleanOldCacheData(yearsToFetch);
    } catch (Exception e) {
      log.error("Failed to initialize holiday cache", e);
    }
  }

  public void fetchAndCacheYearData(int year) {
    String cacheKey = "calendar:" + year;
    Object cachedData = redisTemplate.opsForValue().get(cacheKey);

    if (cachedData != null) {
      log.info("Data for year {} is already cached", year);
      return;
    }

    try {
      log.info("Fetching holiday data for year {}", year);

      String url = String.format(
          "%s/getHoliDeInfo?solYear=%d&ServiceKey=%s&_type=json&numOfRows=100",
          baseUrl, year, serviceKey
      );

      log.debug("Request URL: {}", url);

      String responseBody = webClient.get()
          .uri(url)
          .retrieve()
          .bodyToMono(String.class)
          .doOnError(error -> log.error("WebClient error for year {}: {}", year,
              error.getMessage()))
          .onErrorResume(error -> Mono.empty())
          .block();

      log.debug("API Response Body: {}", responseBody);

      if (Strings.isNullOrEmpty(responseBody)) {
        return;
      }

      JsonNode rootNode = objectMapper.readTree(responseBody);
      JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

      List<HolidayDto> holidays = Lists.newArrayList();
      if (itemsNode.isArray()) {
        for (JsonNode item : itemsNode) {
          holidays.add(objectMapper.treeToValue(item, HolidayDto.class));
        }
      } else if (!itemsNode.isMissingNode()) {
        holidays.add(objectMapper.treeToValue(itemsNode, HolidayDto.class));
      }

      if (!holidays.isEmpty()) {
        redisTemplate.opsForValue().set(cacheKey, holidays);
        log.info("Data for year {} cached successfully with {} holidays", year, holidays.size());
      } else {
        log.warn("No holidays found for year {}", year);
      }
    } catch (Exception e) {
      log.error("Failed to fetch data for year {}: {}", year, e.getMessage(), e);
    }
  }

  public void cleanOldCacheData(List<Integer> validYears) {
    Set<String> keys = redisTemplate.keys("calendar:*");
    if (keys.isEmpty()) {
      log.info("No old cache data to clean");
      return;
    }

    keys.stream()
        .filter(key -> key.contains(":"))
        .forEach(key -> {
          String yearStr = key.split(":")[1];
          int year = Integer.parseInt(yearStr);
          if (!validYears.contains(year)) {
            redisTemplate.delete(key);
            log.info("Old cache data for year {} has been deleted", year);
          }
        });
  }

  @SuppressWarnings("unchecked")
  public List<HolidayDto> getHolidaysByYear(int year) {
    String cacheKey = "calendar:" + year;
    Object cachedData = redisTemplate.opsForValue().get(cacheKey);
    return cachedData != null ? (List<HolidayDto>) cachedData : ImmutableList.of();
  }
}
