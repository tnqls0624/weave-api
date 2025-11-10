package com.weave.domain.location.controller;

import com.weave.domain.location.dto.LocationRequestDto;
import com.weave.domain.location.dto.LocationResponseDto;
import com.weave.domain.location.service.LocationService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationRSocketController {

  private final LocationService locationService;

  // 워크스페이스별 위치 스트림을 관리하는 Sink
  private final Map<String, Sinks.Many<LocationResponseDto>> workspaceStreams = new ConcurrentHashMap<>();

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
      @Payload LocationRequestDto dto) {

    return Mono.fromRunnable(() -> {
      log.info("RSocket: Update location for workspace {}", workspaceId);

      // TODO: 인증 구현 후 userDetails.getUsername() 사용
      // 임시로 첫 번째 사용자 이메일 사용
      String tempEmail = "test@example.com";

      // 위치 저장
      LocationResponseDto response = locationService.saveLocation(workspaceId, dto, tempEmail);

      // 해당 워크스페이스를 구독 중인 모든 클라이언트에게 브로드캐스트
      Sinks.Many<LocationResponseDto> sink = workspaceStreams.get(workspaceId);
      if (sink != null) {
        sink.tryEmitNext(response);
      }
    });
  }

  /**
   * Request-Stream: 워크스페이스의 위치 스트림 구독 클라이언트: rsocket.requestStream()
   */
  @MessageMapping("workspace.{workspaceId}.locations.stream")
  public Flux<LocationResponseDto> streamLocations(
      @DestinationVariable String workspaceId) {

    log.info("RSocket: Stream locations for workspace {}", workspaceId);

    // 워크스페이스별 Sink 생성 (없으면)
    Sinks.Many<LocationResponseDto> sink = workspaceStreams.computeIfAbsent(
        workspaceId,
        k -> Sinks.many().multicast().onBackpressureBuffer()
    );

    // 초기 위치 데이터 + 실시간 스트림
    return Flux.concat(
        Flux.fromIterable(locationService.getLocations(workspaceId)),
        sink.asFlux()
    );
  }

  /**
   * Channel: 양방향 스트리밍 (위치 전송 + 수신) 클라이언트: rsocket.requestChannel()
   */
  @MessageMapping("workspace.{workspaceId}.locations.channel")
  public Flux<LocationResponseDto> channelLocations(
      @DestinationVariable String workspaceId,
      Flux<LocationRequestDto> locationStream) {

    log.info("RSocket: Channel locations for workspace {}", workspaceId);

    // 워크스페이스별 Sink 생성
    Sinks.Many<LocationResponseDto> sink = workspaceStreams.computeIfAbsent(
        workspaceId,
        k -> Sinks.many().multicast().onBackpressureBuffer()
    );

    // TODO: 인증 구현 후 userDetails.getUsername() 사용
    String tempEmail = "test@example.com";

    // 클라이언트로부터 받은 위치를 저장하고 브로드캐스트
    locationStream.subscribe(dto -> {
      LocationResponseDto response = locationService.saveLocation(workspaceId, dto, tempEmail);
      sink.tryEmitNext(response);
    });

    // 초기 위치 + 실시간 스트림 반환
    return Flux.concat(
        Flux.fromIterable(locationService.getLocations(workspaceId)),
        sink.asFlux()
    );
  }
}
