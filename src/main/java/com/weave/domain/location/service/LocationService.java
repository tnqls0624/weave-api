package com.weave.domain.location.service;

import com.weave.domain.location.dto.LocationRequestDto;
import com.weave.domain.location.dto.LocationResponseDto;
import com.weave.domain.location.entity.Location;
import com.weave.domain.location.repository.LocationRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.domain.workspace.repository.WorkspaceRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import java.util.List;
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
  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;

  public LocationResponseDto saveLocation(String workspaceId, LocationRequestDto dto,
      String email) {
    log.info("save location: {}", dto);
    log.info("save location by user: {}", email);
    // 워크스페이스 검증
    // 사용자 검증
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 위치 저장
    Location location = Location.builder()
        .workspaceId(new ObjectId(workspaceId))
        .userId(user.getId())
        .latitude(dto.getLatitude())
        .longitude(dto.getLongitude())
        .build();

    Location savedLocation = locationRepository.save(location);

    return LocationResponseDto.from(savedLocation, user.getName());
  }

  public List<LocationResponseDto> getLocations(String workspaceId) {
    // 워크스페이스의 모든 위치 조회
    List<Location> locations = locationRepository.findByWorkspaceIdOrderByTimestampDesc(
        new ObjectId(workspaceId));

    // 각 사용자별 최신 위치만 필터링
    return locations.stream()
        .collect(Collectors.groupingBy(Location::getUserId))
        .values().stream()
        .map(list -> list.get(0)) // 최신 위치 (timestamp desc로 정렬되어 있음)
        .map(location -> {
          User user = userRepository.findById(location.getUserId())
              .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
          return LocationResponseDto.from(location, user.getName());
        })
        .collect(Collectors.toList());
  }
}
