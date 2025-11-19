package com.weave.domain.phishing.controller;

import com.weave.domain.phishing.dto.PhishingReportRequestDto;
import com.weave.domain.phishing.dto.PhishingReportResponseDto;
import com.weave.domain.phishing.dto.PhishingStatisticsDto;
import com.weave.domain.phishing.service.PhishingGuardService;
import com.weave.domain.phishing.service.PhishingStreamService;
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

/**
 * 피싱 가드 STOMP WebSocket 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PhishingStompController {

  private final PhishingGuardService phishingGuardService;
  private final PhishingStreamService streamService;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 피싱 신고 접수 (WebSocket)
   * 클라이언트 -> 서버: /app/phishing.report
   * 서버 -> 클라이언트: /topic/phishing.alerts (브로드캐스트)
   */
  @MessageMapping("/phishing.report")
  public void reportPhishing(
      @Payload PhishingReportRequestDto dto,
      Principal principal) {

    log.info("STOMP: 피싱 신고 접수 - 사용자: {}, SMS ID: {}", principal.getName(), dto.getSmsId());

    if (principal == null) {
      log.warn("Unauthorized phishing report attempt");
      throw new IllegalStateException("Authentication required");
    }

    try {
      // 피싱 신고 처리
      PhishingReportResponseDto response = phishingGuardService.reportPhishing(principal.getName(), dto);

      // 전체 알림 토픽에 브로드캐스트
      messagingTemplate.convertAndSend("/topic/phishing.alerts", response);

      // 워크스페이스별 알림
      if (dto.getWorkspaceId() != null) {
        messagingTemplate.convertAndSend(
          "/topic/phishing.alerts." + dto.getWorkspaceId(),
          response
        );

        // 워크스페이스 통계 업데이트 알림
        PhishingStatisticsDto stats = phishingGuardService.getWorkspaceStatistics(dto.getWorkspaceId());
        messagingTemplate.convertAndSend(
          "/topic/phishing.stats." + dto.getWorkspaceId(),
          stats
        );
      }

      // 신고자에게 확인 메시지
      messagingTemplate.convertAndSendToUser(
        principal.getName(),
        "/queue/phishing.confirm",
        response
      );

      log.info("피싱 신고 처리 완료 - ID: {}", response.getId());

    } catch (Exception e) {
      log.error("피싱 신고 처리 실패", e);

      // 에러 메시지 전송
      messagingTemplate.convertAndSendToUser(
        principal.getName(),
        "/queue/phishing.error",
        "피싱 신고 처리 중 오류가 발생했습니다: " + e.getMessage()
      );
    }
  }

  /**
   * 피싱 통계 스트림 시작/중지
   * 클라이언트 -> 서버: /app/phishing.stats.stream
   */
  @MessageMapping("/phishing.stats.stream")
  public void controlStatsStream(
      @Payload StatsStreamRequest request,
      Principal principal) {

    log.info("STOMP: 피싱 통계 스트림 제어 - 사용자: {}, 액션: {}",
      principal.getName(), request.getAction());

    if ("start".equals(request.getAction())) {
      streamService.startStatsStream(request.getWorkspaceId(), principal.getName());
    } else if ("stop".equals(request.getAction())) {
      streamService.stopStatsStream(request.getWorkspaceId(), principal.getName());
    }
  }

  /**
   * 워크스페이스 피싱 위치 알림
   * 클라이언트 -> 서버: /app/phishing.location.{workspaceId}
   */
  @MessageMapping("/phishing.location.{workspaceId}")
  public void sendPhishingLocationAlert(
      @DestinationVariable String workspaceId,
      @Payload PhishingLocationAlert alert,
      Principal principal) {

    log.info("STOMP: 피싱 위치 알림 - 워크스페이스: {}, 사용자: {}", workspaceId, principal.getName());

    // 워크스페이스 멤버들에게 위치 알림 브로드캐스트
    messagingTemplate.convertAndSend(
      "/topic/phishing.location." + workspaceId,
      alert
    );

    // 근처 사용자들에게 개별 알림 (선택적)
    if (alert.getLocation() != null) {
      streamService.notifyNearbyUsers(alert);
    }
  }

  /**
   * 피싱 알림 구독 시 초기 데이터 전송
   * 클라이언트가 /topic/phishing.alerts를 구독할 때
   */
  @EventListener
  public void handlePhishingSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();

    if (destination == null) {
      return;
    }

    try {
      // 전체 피싱 알림 구독
      if ("/topic/phishing.alerts".equals(destination)) {
        log.info("STOMP: 새 피싱 알림 구독 - 사용자: {}", accessor.getUser().getName());

        // 최근 피싱 알림 전송 (선택적)
        if (accessor.getUser() != null) {
          List<PhishingReportResponseDto> recentAlerts =
            streamService.getRecentAlerts(accessor.getUser().getName(), 10);

          messagingTemplate.convertAndSendToUser(
            accessor.getUser().getName(),
            "/queue/phishing.recent",
            recentAlerts
          );
        }
      }

      // 워크스페이스별 피싱 알림 구독
      if (destination.startsWith("/topic/phishing.alerts.")) {
        String workspaceId = destination.substring("/topic/phishing.alerts.".length());
        log.info("STOMP: 워크스페이스 피싱 알림 구독 - WS: {}", workspaceId);

        // 워크스페이스 통계 초기 전송
        if (accessor.getUser() != null) {
          PhishingStatisticsDto stats = phishingGuardService.getWorkspaceStatistics(workspaceId);
          messagingTemplate.convertAndSendToUser(
            accessor.getUser().getName(),
            "/queue/phishing.stats.initial",
            stats
          );
        }
      }

      // 피싱 통계 스트림 구독
      if (destination.startsWith("/topic/phishing.stats.")) {
        String workspaceId = destination.substring("/topic/phishing.stats.".length());
        log.info("STOMP: 피싱 통계 스트림 구독 - WS: {}", workspaceId);

        // 통계 스트리밍 시작
        if (accessor.getUser() != null) {
          streamService.startStatsStream(workspaceId, accessor.getUser().getName());
        }
      }

    } catch (Exception e) {
      log.error("피싱 구독 처리 중 오류", e);
    }
  }

  /**
   * 통계 스트림 요청 DTO
   */
  public static class StatsStreamRequest {
    private String workspaceId;
    private String action; // start, stop

    // Getters and setters
    public String getWorkspaceId() {
      return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
      this.workspaceId = workspaceId;
    }

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }
  }

  /**
   * 피싱 위치 알림 DTO
   */
  public static class PhishingLocationAlert {
    private String smsId;
    private String sender;
    private String riskLevel;
    private LocationDto location;
    private Long timestamp;

    // Getters and setters
    public String getSmsId() {
      return smsId;
    }

    public void setSmsId(String smsId) {
      this.smsId = smsId;
    }

    public String getSender() {
      return sender;
    }

    public void setSender(String sender) {
      this.sender = sender;
    }

    public String getRiskLevel() {
      return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
      this.riskLevel = riskLevel;
    }

    public LocationDto getLocation() {
      return location;
    }

    public void setLocation(LocationDto location) {
      this.location = location;
    }

    public Long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
    }

    public static class LocationDto {
      private Double latitude;
      private Double longitude;

      // Getters and setters
      public Double getLatitude() {
        return latitude;
      }

      public void setLatitude(Double latitude) {
        this.latitude = latitude;
      }

      public Double getLongitude() {
        return longitude;
      }

      public void setLongitude(Double longitude) {
        this.longitude = longitude;
      }
    }
  }
}