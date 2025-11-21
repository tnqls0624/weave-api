package com.weave.domain.phishing.service;

import com.weave.domain.phishing.controller.PhishingStompController.PhishingLocationAlert;
import com.weave.domain.phishing.dto.PhishingReportResponseDto;
import com.weave.domain.phishing.dto.PhishingStatisticsDto;
import com.weave.domain.phishing.entity.PhishingReport;
import com.weave.domain.phishing.entity.PhishingStatistics;
import com.weave.domain.phishing.repository.PhishingReportRepository;
import com.weave.domain.phishing.repository.PhishingStatisticsRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 피싱 실시간 스트리밍 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhishingStreamService {

  private final PhishingReportRepository reportRepository;
  private final PhishingStatisticsRepository statisticsRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final UserRepository userRepository;

  // 활성 스트림 관리
  private final Map<String, StreamInfo> activeStreams = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

  /**
   * 통계 스트림 시작
   */
  public void startStatsStream(String workspaceId, String userId) {
    String streamKey = generateStreamKey(workspaceId, userId);

    if (activeStreams.containsKey(streamKey)) {
      log.debug("Stats stream already active for: {}", streamKey);
      return;
    }

    log.info("Starting stats stream for workspace: {}, user: {}", workspaceId, userId);

    // 5초마다 통계 업데이트 전송
    ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
      try {
        sendStatsUpdate(workspaceId, userId);
      } catch (Exception e) {
        log.error("Error sending stats update", e);
      }
    }, 0, 5, TimeUnit.SECONDS);

    StreamInfo streamInfo = new StreamInfo(workspaceId, userId, future);
    activeStreams.put(streamKey, streamInfo);
  }

  /**
   * 통계 스트림 중지
   */
  public void stopStatsStream(String workspaceId, String userId) {
    String streamKey = generateStreamKey(workspaceId, userId);
    StreamInfo streamInfo = activeStreams.remove(streamKey);

    if (streamInfo != null) {
      streamInfo.getFuture().cancel(true);
      log.info("Stopped stats stream for: {}", streamKey);
    }
  }

  /**
   * 통계 업데이트 전송
   */
  private void sendStatsUpdate(String workspaceId, String userId) {
    try {
      PhishingStatisticsDto stats;

      if (isValidObjectId(workspaceId)) {
        // 워크스페이스 통계
        Optional<PhishingStatistics> wsStats = statisticsRepository
            .findByWorkspaceIdAndDateAndStatType(
                new ObjectId(workspaceId),
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "daily"
            );

        stats = PhishingStatisticsDto.from(wsStats.orElse(null));

        messagingTemplate.convertAndSend(
            "/topic/phishing.stats." + workspaceId,
            stats
        );
      }

      if (isValidObjectId(userId)) {
        // 사용자 통계
        Optional<PhishingStatistics> userStats = statisticsRepository
            .findByUserIdAndDateAndStatType(
                new ObjectId(userId),
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "daily"
            );

        stats = PhishingStatisticsDto.from(userStats.orElse(null));

        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/phishing.stats",
            stats
        );
      }
    } catch (Exception e) {
      log.error("Failed to send stats update", e);
    }
  }

  /**
   * 최근 알림 조회
   */
  public List<PhishingReportResponseDto> getRecentAlerts(String email, int limit) {
    try {
      Optional<User> user = this.userRepository.findByEmail(email);
      if (user.isEmpty()) {
        log.warn("User not found for email: {}", email);
        return new ArrayList<>();
      }
      List<PhishingReport> reports = reportRepository.findByUserId(
          user.get().getId(),
          PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"))
      );

      return reports.stream()
          .map(PhishingReportResponseDto::from)
          .toList();
    } catch (Exception e) {
      log.error("Failed to get recent alerts for user:", e);
      return new ArrayList<>();
    }
  }

  /**
   * 근처 사용자에게 알림 전송
   */
  public void notifyNearbyUsers(PhishingLocationAlert alert) {
    if (alert.getLocation() == null) {
      return;
    }

    try {
      // 근처 피싱 신고 조회 (5km 반경)
      double latitude = alert.getLocation().getLatitude();
      double longitude = alert.getLocation().getLongitude();
      double radius = 5000; // meters

      // 위도/경도 범위 계산 (1도 ≈ 111km)
      double latDelta = radius / 111000.0;
      double lonDelta = radius / 111000.0;

      double minLat = latitude - latDelta;
      double maxLat = latitude + latDelta;
      double minLon = longitude - lonDelta;
      double maxLon = longitude + lonDelta;

      List<PhishingReport> nearbyReports = reportRepository.findNearbyReports(
          minLat, maxLat, minLon, maxLon
      );

      // 고유한 사용자 ID 추출
      Set<String> nearbyUserIds = new HashSet<>();
      for (PhishingReport report : nearbyReports) {
        if (report.getUserId() != null) {
          nearbyUserIds.add(report.getUserId().toString());
        }
      }

      // 각 사용자에게 알림 전송
      for (String userId : nearbyUserIds) {
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/phishing.nearby",
            alert
        );
      }

      log.info("Notified {} nearby users about phishing alert", nearbyUserIds.size());

    } catch (Exception e) {
      log.error("Failed to notify nearby users", e);
    }
  }

  /**
   * 실시간 대시보드 스트림 시작
   */
  public void startDashboardStream(String workspaceId) {
    String streamKey = "dashboard:" + workspaceId;

    if (activeStreams.containsKey(streamKey)) {
      return;
    }

    log.info("Starting dashboard stream for workspace: {}", workspaceId);

    // 10초마다 대시보드 데이터 업데이트
    ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
      try {
        sendDashboardUpdate(workspaceId);
      } catch (Exception e) {
        log.error("Error sending dashboard update", e);
      }
    }, 0, 10, TimeUnit.SECONDS);

    activeStreams.put(streamKey, new StreamInfo(workspaceId, null, future));
  }

  /**
   * 대시보드 업데이트 전송
   */
  private void sendDashboardUpdate(String workspaceId) {
    try {
      if (!isValidObjectId(workspaceId)) {
        log.warn("Invalid workspaceId for dashboard update: {}", workspaceId);
        return;
      }

      // 최근 24시간 통계 조회
      LocalDate today = LocalDate.now();
      List<PhishingStatistics> hourlyStats = statisticsRepository
          .findWorkspaceStatsByDateRange(
              new ObjectId(workspaceId),
              today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
              today.format(DateTimeFormatter.ISO_LOCAL_DATE),
              "hourly"
          );

      // 대시보드 데이터 구성
      Map<String, Object> dashboardData = new HashMap<>();
      dashboardData.put("hourlyStats", hourlyStats);
      dashboardData.put("timestamp", System.currentTimeMillis());

      // 최근 고위험 알림
      List<PhishingReport> highRiskReports = reportRepository
          .findByWorkspaceIdAndRiskLevel(
              new ObjectId(workspaceId),
              "high",
              PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"))
          );

      dashboardData.put("recentHighRisk", highRiskReports.stream()
          .map(PhishingReportResponseDto::from)
          .toList());

      // 브로드캐스트
      messagingTemplate.convertAndSend(
          "/topic/phishing.dashboard." + workspaceId,
          dashboardData
      );

    } catch (Exception e) {
      log.error("Failed to send dashboard update", e);
    }
  }

  /**
   * 실시간 히트맵 데이터 스트리밍
   */
  public void streamHeatmapData(String workspaceId) {
    try {
      if (!isValidObjectId(workspaceId)) {
        log.warn("Invalid workspaceId for heatmap data: {}", workspaceId);
        return;
      }

      // 최근 7일간의 위치 기반 피싱 데이터
      LocalDate endDate = LocalDate.now();
      LocalDate startDate = endDate.minusDays(7);

      List<PhishingReport> reports = reportRepository.findByWorkspaceIdAndDateRange(
          new ObjectId(workspaceId),
          Date.from(startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)),
          Date.from(endDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
      );

      // 위치별 집계
      Map<String, Integer> heatmapData = new HashMap<>();
      for (PhishingReport report : reports) {
        if (report.getLocation() != null
            && report.getLocation().getLatitude() != null
            && report.getLocation().getLongitude() != null) {
          String key = String.format("%.4f,%.4f",
              report.getLocation().getLatitude(),
              report.getLocation().getLongitude());
          heatmapData.merge(key, 1, Integer::sum);
        }
      }

      messagingTemplate.convertAndSend(
          "/topic/phishing.heatmap." + workspaceId,
          heatmapData
      );

    } catch (Exception e) {
      log.error("Failed to stream heatmap data", e);
    }
  }

  /**
   * 모든 스트림 정리
   */
  public void cleanup() {
    log.info("Cleaning up {} active streams", activeStreams.size());

    activeStreams.values().forEach(streamInfo -> {
      streamInfo.getFuture().cancel(true);
    });

    activeStreams.clear();
    scheduler.shutdown();

    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * 스트림 키 생성
   */
  private String generateStreamKey(String workspaceId, String userId) {
    return String.format("%s:%s",
        workspaceId != null ? workspaceId : "null",
        userId != null ? userId : "null"
    );
  }

  /**
   * ObjectId 유효성 검사
   */
  private boolean isValidObjectId(String id) {
    if (id == null || id.isEmpty() || id.equals("null")) {
      return false;
    }

    // ObjectId는 24자리 16진수 문자열이어야 함
    if (id.length() != 24) {
      return false;
    }

    // 16진수 문자인지 확인
    for (char c : id.toCharArray()) {
      if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
        return false;
      }
    }

    return true;
  }

  /**
   * 스트림 정보 클래스
   */
  private static class StreamInfo {

    private final String workspaceId;
    private final String userId;
    private final ScheduledFuture<?> future;

    public StreamInfo(String workspaceId, String userId, ScheduledFuture<?> future) {
      this.workspaceId = workspaceId;
      this.userId = userId;
      this.future = future;
    }

    public ScheduledFuture<?> getFuture() {
      return future;
    }
  }
}