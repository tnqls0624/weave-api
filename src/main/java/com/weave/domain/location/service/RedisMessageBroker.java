package com.weave.domain.location.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weave.domain.location.dto.LocationResponseDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
public class RedisMessageBroker {

  private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
  private final ObjectMapper objectMapper;

  public RedisMessageBroker(
      ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
      @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
    this.reactiveRedisTemplate = reactiveRedisTemplate;
    this.objectMapper = objectMapper;
  }

  // 워크스페이스별 로컬 Sink (현재 서버에 연결된 클라이언트들에게 브로드캐스트)
  private final Map<String, Sinks.Many<LocationResponseDto>> localSinks = new ConcurrentHashMap<>();

  // Redis 구독 관리
  private final Map<String, ReactiveSubscription> subscriptions = new ConcurrentHashMap<>();

  private static final String CHANNEL_PREFIX = "workspace:location:";

  /**
   * 워크스페이스의 위치 스트림을 구독
   */
  public Flux<LocationResponseDto> subscribe(String workspaceId) {
    log.info("Subscribing to workspace location stream: {}", workspaceId);

    // 로컬 Sink 생성 또는 재사용
    Sinks.Many<LocationResponseDto> sink = localSinks.computeIfAbsent(
        workspaceId,
        k -> {
          // Redis 채널 구독 시작
          subscribeToRedis(workspaceId);
          return Sinks.many().multicast().onBackpressureBuffer();
        }
    );

    return sink.asFlux();
  }

  /**
   * Redis 채널을 구독하여 다른 서버의 메시지를 받음
   */
  private void subscribeToRedis(String workspaceId) {
    String channel = CHANNEL_PREFIX + workspaceId;

    if (subscriptions.containsKey(workspaceId)) {
      log.debug("Already subscribed to Redis channel: {}", channel);
      return;
    }

    reactiveRedisTemplate
        .listenTo(ChannelTopic.of(channel))
        .map(ReactiveSubscription.Message::getMessage)
        .subscribe(
            message -> {
              try {
                LocationResponseDto location = objectMapper.readValue(message, LocationResponseDto.class);
                Sinks.Many<LocationResponseDto> sink = localSinks.get(workspaceId);
                if (sink != null) {
                  sink.tryEmitNext(location);
                }
              } catch (JsonProcessingException e) {
                log.error("Failed to deserialize location message from Redis: {}", message, e);
              }
            },
            error -> log.error("Error in Redis subscription for workspace {}", workspaceId, error),
            () -> log.info("Redis subscription completed for workspace {}", workspaceId)
        );

    log.info("Started Redis subscription for channel: {}", channel);
  }

  /**
   * 위치 업데이트를 Redis로 publish (모든 서버에 브로드캐스트)
   */
  public void publish(String workspaceId, LocationResponseDto location) {
    String channel = CHANNEL_PREFIX + workspaceId;

    try {
      String message = objectMapper.writeValueAsString(location);
      reactiveRedisTemplate
          .convertAndSend(channel, message)
          .subscribe(
              count -> log.debug("Published location to {} subscribers on channel: {}", count, channel),
              error -> log.error("Failed to publish location to Redis channel: {}", channel, error)
          );
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize location for Redis publish", e);
    }
  }

  /**
   * 워크스페이스 구독 해제
   */
  public void unsubscribe(String workspaceId) {
    localSinks.remove(workspaceId);
    ReactiveSubscription subscription = subscriptions.remove(workspaceId);
    if (subscription != null) {
      subscription.cancel();
      log.info("Unsubscribed from workspace: {}", workspaceId);
    }
  }

  /**
   * 로컬 Sink 가져오기 (기존 코드 호환성을 위해)
   */
  public Sinks.Many<LocationResponseDto> getLocalSink(String workspaceId) {
    return localSinks.computeIfAbsent(
        workspaceId,
        k -> {
          subscribeToRedis(workspaceId);
          return Sinks.many().multicast().onBackpressureBuffer();
        }
    );
  }

  @PreDestroy
  public void cleanup() {
    log.info("Cleaning up Redis subscriptions");
    subscriptions.values().forEach(ReactiveSubscription::cancel);
    subscriptions.clear();
    localSinks.clear();
  }
}
