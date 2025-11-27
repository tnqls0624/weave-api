package com.weave.domain.location.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weave.domain.location.dto.LocationResponseDto;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
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
  private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();

  // 구독자 수 추적 (메모리 누수 방지)
  private final Map<String, AtomicInteger> subscriberCounts = new ConcurrentHashMap<>();

  // 마지막 활동 시간 추적 (유휴 구독 정리)
  private final Map<String, Long> lastActivityTime = new ConcurrentHashMap<>();

  private static final String CHANNEL_PREFIX = "workspace:location:";
  private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5분 유휴 시간 후 정리

  /**
   * 워크스페이스의 위치 스트림을 구독
   */
  public Flux<LocationResponseDto> subscribe(String workspaceId) {
    log.info("Subscribing to workspace location stream: {}", workspaceId);

    // 구독자 수 증가
    subscriberCounts.computeIfAbsent(workspaceId, k -> new AtomicInteger(0)).incrementAndGet();
    lastActivityTime.put(workspaceId, System.currentTimeMillis());

    // 로컬 Sink 생성 또는 재사용
    Sinks.Many<LocationResponseDto> sink = localSinks.computeIfAbsent(
        workspaceId,
        k -> {
          // Redis 채널 구독 시작
          subscribeToRedis(workspaceId);
          return Sinks.many().multicast().onBackpressureBuffer(256);
        }
    );

    return sink.asFlux()
        .doOnCancel(() -> handleUnsubscribe(workspaceId))
        .doOnTerminate(() -> handleUnsubscribe(workspaceId))
        .doOnNext(location -> lastActivityTime.put(workspaceId, System.currentTimeMillis()));
  }

  /**
   * 구독 해제 처리 (참조 카운팅)
   */
  private void handleUnsubscribe(String workspaceId) {
    AtomicInteger count = subscriberCounts.get(workspaceId);
    if (count != null) {
      int remaining = count.decrementAndGet();
      log.debug("Subscriber disconnected from workspace {}, remaining: {}", workspaceId, remaining);

      // 모든 구독자가 떠나면 정리
      if (remaining <= 0) {
        log.info("No more subscribers for workspace {}, scheduling cleanup", workspaceId);
        // 즉시 정리하지 않고 잠시 대기 (재연결 대비)
        // 실제 정리는 scheduled task에서 수행
      }
    }
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

    Disposable subscription = reactiveRedisTemplate
        .listenTo(ChannelTopic.of(channel))
        .map(ReactiveSubscription.Message::getMessage)
        .doOnNext(message -> {
          try {
            LocationResponseDto location = objectMapper.readValue(message, LocationResponseDto.class);
            Sinks.Many<LocationResponseDto> sink = localSinks.get(workspaceId);
            if (sink != null) {
              Sinks.EmitResult result = sink.tryEmitNext(location);
              if (result.isFailure()) {
                log.warn("Failed to emit location to sink: {}", result);
              }
            }
          } catch (JsonProcessingException e) {
            log.error("Failed to deserialize location message from Redis: {}", message, e);
          }
        })
        .doOnError(error -> log.error("Error in Redis subscription for workspace {}", workspaceId, error))
        .doOnComplete(() -> {
          log.info("Redis subscription completed for workspace {}", workspaceId);
          subscriptions.remove(workspaceId);
        })
        .subscribe();

    subscriptions.put(workspaceId, subscription);
    log.info("Started Redis subscription for channel: {}", channel);
  }

  /**
   * 위치 업데이트를 Redis로 publish (모든 서버에 브로드캐스트)
   */
  public void publish(String workspaceId, LocationResponseDto location) {
    String channel = CHANNEL_PREFIX + workspaceId;
    lastActivityTime.put(workspaceId, System.currentTimeMillis());

    try {
      String message = objectMapper.writeValueAsString(location);
      reactiveRedisTemplate
          .convertAndSend(channel, message)
          .timeout(Duration.ofSeconds(5))
          .subscribe(
              count -> log.debug("Published location to {} subscribers on channel: {}", count, channel),
              error -> log.error("Failed to publish location to Redis channel: {}", channel, error)
          );
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize location for Redis publish", e);
    }
  }

  /**
   * 워크스페이스 구독 해제 (강제)
   */
  public void unsubscribe(String workspaceId) {
    log.info("Force unsubscribing from workspace: {}", workspaceId);

    localSinks.remove(workspaceId);
    subscriberCounts.remove(workspaceId);
    lastActivityTime.remove(workspaceId);

    Disposable subscription = subscriptions.remove(workspaceId);
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
      log.info("Unsubscribed from workspace: {}", workspaceId);
    }
  }

  /**
   * 로컬 Sink 가져오기 (기존 코드 호환성을 위해)
   */
  public Sinks.Many<LocationResponseDto> getLocalSink(String workspaceId) {
    lastActivityTime.put(workspaceId, System.currentTimeMillis());
    return localSinks.computeIfAbsent(
        workspaceId,
        k -> {
          subscribeToRedis(workspaceId);
          return Sinks.many().multicast().onBackpressureBuffer(256);
        }
    );
  }

  /**
   * 유휴 구독 정리 (5분마다 실행)
   */
  @Scheduled(fixedRate = 300000) // 5분
  public void cleanupIdleSubscriptions() {
    long now = System.currentTimeMillis();
    int cleaned = 0;

    for (Map.Entry<String, Long> entry : lastActivityTime.entrySet()) {
      String workspaceId = entry.getKey();
      long lastActivity = entry.getValue();

      // 5분 이상 활동이 없고, 구독자가 없는 경우 정리
      AtomicInteger count = subscriberCounts.get(workspaceId);
      if ((now - lastActivity > IDLE_TIMEOUT_MS) && (count == null || count.get() <= 0)) {
        log.info("Cleaning up idle subscription for workspace: {}", workspaceId);
        unsubscribe(workspaceId);
        cleaned++;
      }
    }

    if (cleaned > 0) {
      log.info("Cleaned up {} idle subscriptions. Active subscriptions: {}", cleaned, subscriptions.size());
    }
  }

  /**
   * 현재 활성 구독 상태 조회 (모니터링용)
   */
  public Map<String, Object> getSubscriptionStatus() {
    Map<String, Object> status = new ConcurrentHashMap<>();
    status.put("activeSubscriptions", subscriptions.size());
    status.put("activeSinks", localSinks.size());

    Map<String, Integer> subscriberStatus = new ConcurrentHashMap<>();
    subscriberCounts.forEach((k, v) -> subscriberStatus.put(k, v.get()));
    status.put("subscriberCounts", subscriberStatus);

    return status;
  }

  @PreDestroy
  public void cleanup() {
    log.info("Cleaning up Redis subscriptions");
    subscriptions.values().forEach(Disposable::dispose);
    subscriptions.clear();
    localSinks.clear();
    subscriberCounts.clear();
    lastActivityTime.clear();
  }
}
