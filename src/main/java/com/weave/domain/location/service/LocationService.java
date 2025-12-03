package com.weave.domain.location.service;

import com.weave.domain.location.dto.LocationRequestDto;
import com.weave.domain.location.dto.LocationResponseDto;
import com.weave.domain.location.entity.Location;
import com.weave.domain.location.repository.LocationRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LocationService {

  private final LocationRepository locationRepository;
  private final UserRepository userRepository;

  public LocationResponseDto saveLocation(String workspaceId, LocationRequestDto dto,
      String email) {
    log.debug("save location: {}", dto);
    // 사용자 검증
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 위치 upsert (사용자별 1개 문서만 유지)
    Location savedLocation = locationRepository.upsertLocation(
        new ObjectId(workspaceId),
        user.getId(),
        dto.getLatitude(),
        dto.getLongitude()
    );

    return LocationResponseDto.from(savedLocation, user.getName());
  }

  public List<LocationResponseDto> getLocations(String workspaceId) {
    // 최적화: Aggregation으로 사용자별 최신 위치만 조회 (DB 레벨에서 처리)
    List<Location> latestLocations = locationRepository.findLatestLocationsByWorkspaceId(
        new ObjectId(workspaceId));

    if (latestLocations.isEmpty()) {
      return Collections.emptyList();
    }

    // 최적화: 모든 사용자 ID를 모아서 한 번에 조회 (N+1 문제 해결)
    Set<ObjectId> userIds = latestLocations.stream()
        .map(Location::getUserId)
        .collect(Collectors.toSet());

    Map<ObjectId, User> userMap = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

    // 위치 데이터와 사용자 정보 매핑
    return latestLocations.stream()
        .map(location -> {
          User user = userMap.get(location.getUserId());
          String userName = user != null ? user.getName() : "Unknown";
          return LocationResponseDto.from(location, userName);
        })
        .collect(Collectors.toList());
  }
}
