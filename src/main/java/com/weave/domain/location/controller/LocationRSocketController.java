package com.weave.domain.location.controller;

import com.weave.domain.location.dto.LocationRequestDto;
import com.weave.domain.location.dto.LocationResponseDto;
import com.weave.domain.location.service.LocationService;
import com.weave.domain.location.service.RedisMessageBroker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationRSocketController {

  private final LocationService locationService;
  private final RedisMessageBroker redisMessageBroker;

  /**
   * Request-Response: 워크스페이스의 현재 위치 조회 클라이언트: rsocket.requestResponse()
   */
  @MessageMapping("workspace.{workspaceId}.locations.get")
  public Mono<List<LocationResponseDto>> getLocations(
      @DestinationVariable String workspaceId) {
    log.info("RSocket: Get locations for workspace {}", workspaceId);
    return Mono.fromCallable(() -> locationService.getLocations(workspaceId));
  }

  /**
   * Fire-and-Forget: 위치 업데이트 전송 클라이언트: rsocket.fireAndForget()
   */
  @MessageMapping("workspace.{workspaceId}.location.update")
  public Mono<Void> updateLocation(
      @DestinationVariable String workspaceId,
      @Payload LocationRequestDto dto,
      @AuthenticationPrincipal UserDetails userDetails) {

    return Mono.fromRunnable(() -> {
      log.info("RSocket: Update location for workspace {}", workspaceId);

      String userEmail = userDetails.getUsername();

      // 위치 저장
      LocationResponseDto response = locationService.saveLocation(workspaceId, dto, userEmail);

      // Redis를 통해 모든 서버의 클라이언트에게 브로드캐스트
      redisMessageBroker.publish(workspaceId, response);
    });
  }

  /**
   * Request-Stream: 워크스페이스의 위치 스트림 구독 클라이언트: rsocket.requestStream()
   */
  @MessageMapping("workspace.{workspaceId}.locations.stream")
  public Flux<LocationResponseDto> streamLocations(
      @DestinationVariable String workspaceId) {

    log.info("RSocket: Stream locations for workspace {}", workspaceId);

    // 초기 위치 데이터 + Redis를 통한 실시간 스트림
    return Flux.concat(
        Flux.fromIterable(locationService.getLocations(workspaceId)),
        redisMessageBroker.subscribe(workspaceId)
    );
  }

  /**
   * Channel: 양방향 스트리밍 (위치 전송 + 수신) 클라이언트: rsocket.requestChannel()
   */
  @MessageMapping("workspace.{workspaceId}.locations.channel")
  public Flux<LocationResponseDto> channelLocations(
      @DestinationVariable String workspaceId,
      Flux<LocationRequestDto> locationStream,
      @AuthenticationPrincipal UserDetails userDetails) {

    log.info("RSocket: Channel locations for workspace {}", workspaceId);

    String userEmail = userDetails.getUsername();

    // 클라이언트로부터 받은 위치를 저장하고 Redis를 통해 브로드캐스트
    locationStream.subscribe(dto -> {
      LocationResponseDto response = locationService.saveLocation(workspaceId, dto, userEmail);
      redisMessageBroker.publish(workspaceId, response);
    });

    // 초기 위치 + Redis를 통한 실시간 스트림 반환
    return Flux.concat(
        Flux.fromIterable(locationService.getLocations(workspaceId)),
        redisMessageBroker.subscribe(workspaceId)
    );
  }
}
