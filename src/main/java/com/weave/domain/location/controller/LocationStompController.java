package com.weave.domain.location.controller;

import com.weave.domain.location.dto.LocationRequestDto;
import com.weave.domain.location.dto.LocationResponseDto;
import com.weave.domain.location.service.LocationService;
import com.weave.domain.location.service.RedisMessageBroker;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationStompController {

  private final LocationService locationService;
  private final RedisMessageBroker redisMessageBroker;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 위치 업데이트 수신 클라이언트 -> 서버: /app/workspace/{workspaceId}/location
   */
  @MessageMapping("/workspace/{workspaceId}/location")
  public void updateLocation(
      @DestinationVariable String workspaceId,
      @Payload LocationRequestDto dto,
      Principal principal) {

    log.info("STOMP: Update location for workspace {}", workspaceId);

    if (principal == null) {
      log.warn("Unauthorized location update attempt for workspace {}", workspaceId);
      throw new IllegalStateException("Authentication required");
    }

    String userEmail = principal.getName();

    // 위치 저장
    LocationResponseDto response = locationService.saveLocation(workspaceId, dto, userEmail);

    // Redis를 통해 모든 서버의 클라이언트에게 브로드캐스트
    redisMessageBroker.publish(workspaceId, response);
  }

  /**
   * 워크스페이스의 현재 위치 조회 클라이언트 -> 서버: /app/workspace/{workspaceId}/locations 서버 -> 클라이언트:
   * /topic/workspace/{workspaceId}/locations
   */
  @MessageMapping("/workspace/{workspaceId}/locations")
  public void getLocations(
      @DestinationVariable String workspaceId,
      Principal principal) {

    log.info("STOMP: Get locations for workspace {}", workspaceId);

    if (principal == null) {
      throw new IllegalStateException("Authentication required");
    }

    List<LocationResponseDto> locations = locationService.getLocations(workspaceId);

    // 요청한 사용자에게만 전송 (사용자 목적지)
    messagingTemplate.convertAndSendToUser(
        principal.getName(),
        "/queue/locations",
        locations
    );
  }

  /**
   * 워크스페이스 위치 스트림 구독 시 초기 데이터 전송 클라이언트가 /topic/workspace/{workspaceId}/locations를 구독할 때
   */
  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();

    if (destination != null && destination.startsWith("/topic/workspace/") && destination.endsWith(
        "/locations")) {
      // destination: /topic/workspace/{workspaceId}/locations
      String[] parts = destination.split("/");
      if (parts.length >= 4) {
        String workspaceId = parts[3];

        log.info("STOMP: New subscription to workspace {}", workspaceId);

        // 초기 위치 데이터 전송
        List<LocationResponseDto> locations = locationService.getLocations(workspaceId);
        if (accessor.getUser() != null) {
          messagingTemplate.convertAndSendToUser(
              accessor.getUser().getName(),
              "/queue/initial-locations",
              locations
          );
        } else {
          log.warn("Subscribe event without user principal for workspace {}", workspaceId);
        }

        // Redis 구독 시작 (자동으로 브로드캐스트됨)
        subscribeToRedis(workspaceId);
      }
    }
  }

  /**
   * Redis에서 받은 위치 업데이트를 STOMP 클라이언트들에게 브로드캐스트
   */
  private void subscribeToRedis(String workspaceId) {
    redisMessageBroker.subscribe(workspaceId)
        .subscribe(
            location -> {
              // 워크스페이스를 구독 중인 모든 클라이언트에게 브로드캐스트
              messagingTemplate.convertAndSend(
                  "/topic/workspace/" + workspaceId + "/locations",
                  location
              );
            },
            error -> log.error("Error in Redis subscription for workspace {}", workspaceId, error)
        );
  }
}
