package com.weave.domain.phishing.service;

import com.weave.domain.phishing.entity.PhishingReport;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.domain.workspace.entity.Workspace;
import com.weave.domain.workspace.repository.WorkspaceRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 피싱 알림 서비스 FCM을 통한 푸시 알림 전송 (WebClient 비동기 방식으로 성능 최적화)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhishingNotificationService {

  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WebClient webClient;

  @Value("${fcm.server.key:}")
  private String fcmServerKey;

  @Value("${fcm.api.url:https://fcm.googleapis.com/fcm/send}")
  private String fcmApiUrl;

  /**
   * 고위험 피싱 알림 전송
   */
  @Async("notificationExecutor")
  public void sendHighRiskAlert(PhishingReport report) {
    try {
      // 신고자 정보 조회
      Optional<User> userOpt = userRepository.findById(report.getUserId());
      if (userOpt.isEmpty()) {
        log.warn("User not found for phishing report: {}", report.getId());
        return;
      }

      User user = userOpt.get();
      String title = "⚠️ 고위험 피싱 탐지";
      String body = String.format("발신자 %s 로부터 고위험 피싱 메시지가 탐지되었습니다.", report.getSender());

      // 사용자에게 알림
      sendPushNotificationAsync(user.getFcmToken(), title, body, createDataPayload(report))
          .subscribe();

      // 워크스페이스 멤버들에게도 알림
      if (report.getWorkspaceId() != null) {
        notifyWorkspaceMembers(report.getWorkspaceId(), title, body, report);
      }

    } catch (Exception e) {
      log.error("Failed to send high risk alert", e);
    }
  }

  /**
   * 워크스페이스 멤버 알림 (비동기 병렬 처리)
   */
  @Async("notificationExecutor")
  public void notifyWorkspaceMembers(ObjectId workspaceId, String title, String body,
      PhishingReport report) {
    try {
      Optional<Workspace> workspaceOpt = workspaceRepository.findById(workspaceId);
      if (workspaceOpt.isEmpty()) {
        return;
      }

      Workspace workspace = workspaceOpt.get();
      Set<ObjectId> memberIds = workspace.getUsers().stream()
          .filter(id -> !id.equals(report.getUserId()))  // 본인 제외
          .collect(Collectors.toSet());

      if (memberIds.isEmpty()) {
        return;
      }

      // 모든 멤버 한 번에 조회 (N+1 방지)
      List<User> members = userRepository.findAllById(memberIds);
      Map<String, Object> dataPayload = createDataPayload(report);

      // 비동기 병렬 알림 전송
      Flux.fromIterable(members)
          .filter(member -> member.getFcmToken() != null && !member.getFcmToken().isEmpty())
          .flatMap(member -> sendPushNotificationAsync(member.getFcmToken(), title, body, dataPayload)
              .onErrorResume(e -> {
                log.error("Failed to notify workspace member: {}", member.getId(), e);
                return Mono.empty();
              }))
          .subscribeOn(Schedulers.boundedElastic())
          .subscribe();

    } catch (Exception e) {
      log.error("Failed to notify workspace members", e);
    }
  }

  /**
   * 통계 알림 전송 (일일/주간 리포트)
   */
  @Async("notificationExecutor")
  public void sendStatisticsReport(String userId, Map<String, Object> stats, String period) {
    try {
      Optional<User> userOpt = userRepository.findById(new ObjectId(userId));
      if (userOpt.isEmpty()) {
        return;
      }

      User user = userOpt.get();
      String title = period.equals("daily") ? "📊 일일 피싱 리포트" : "📊 주간 피싱 리포트";

      Long totalScanned = (Long) stats.getOrDefault("totalScanned", 0L);
      Long phishingDetected = (Long) stats.getOrDefault("phishingDetected", 0L);
      Double detectionRate = (Double) stats.getOrDefault("detectionRate", 0.0);

      String body = String.format("검사: %d건, 탐지: %d건 (탐지율: %.1f%%)",
          totalScanned, phishingDetected, detectionRate * 100);

      Map<String, Object> data = new HashMap<>();
      data.put("type", "statistics_report");
      data.put("period", period);
      data.putAll(stats);

      sendPushNotificationAsync(user.getFcmToken(), title, body, data).subscribe();

    } catch (Exception e) {
      log.error("Failed to send statistics report", e);
    }
  }

  /**
   * 근처 피싱 알림
   */
  @Async("notificationExecutor")
  public void sendNearbyPhishingAlert(String userId, List<PhishingReport> nearbyReports) {
    try {
      Optional<User> userOpt = userRepository.findById(new ObjectId(userId));
      if (userOpt.isEmpty()) {
        return;
      }

      User user = userOpt.get();
      String title = "📍 근처 피싱 주의";
      String body = String.format("현재 위치 근처에서 %d건의 피싱이 신고되었습니다.", nearbyReports.size());

      Map<String, Object> data = new HashMap<>();
      data.put("type", "nearby_alert");
      data.put("count", nearbyReports.size());
      data.put("reports", nearbyReports.stream()
          .limit(5)  // 최대 5개만
          .map(report -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", report.getId().toString());
            summary.put("sender", report.getSender());
            summary.put("riskLevel", report.getRiskLevel());
            return summary;
          })
          .toList());

      sendPushNotificationAsync(user.getFcmToken(), title, body, data).subscribe();

    } catch (Exception e) {
      log.error("Failed to send nearby phishing alert", e);
    }
  }

  /**
   * 패턴 업데이트 알림 (관리자용)
   */
  @Async("notificationExecutor")
  public void sendPatternUpdateNotification(List<String> adminIds, String action,
      String patternName) {
    String title = "🔧 피싱 패턴 업데이트";
    String body = String.format("패턴 '%s'이(가) %s되었습니다.", patternName,
        action.equals("create") ? "추가" : action.equals("update") ? "수정" : "삭제");

    Map<String, Object> data = new HashMap<>();
    data.put("type", "pattern_update");
    data.put("action", action);
    data.put("patternName", patternName);

    // 모든 관리자 한 번에 조회
    Set<ObjectId> adminObjectIds = adminIds.stream()
        .map(ObjectId::new)
        .collect(Collectors.toSet());
    List<User> admins = userRepository.findAllById(adminObjectIds);

    // 비동기 병렬 알림 전송
    Flux.fromIterable(admins)
        .filter(admin -> admin.getFcmToken() != null && !admin.getFcmToken().isEmpty())
        .flatMap(admin -> sendPushNotificationAsync(admin.getFcmToken(), title, body, data)
            .onErrorResume(e -> {
              log.error("Failed to notify admin: {}", admin.getId(), e);
              return Mono.empty();
            }))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
  }

  /**
   * FCM 푸시 알림 전송 (비동기 WebClient 사용)
   */
  private Mono<Void> sendPushNotificationAsync(String fcmToken, String title, String body,
      Map<String, Object> data) {
    if (fcmToken == null || fcmToken.isEmpty()) {
      log.debug("No FCM token available for notification");
      return Mono.empty();
    }

    if (fcmServerKey == null || fcmServerKey.isEmpty()) {
      log.warn("FCM server key not configured");
      return Mono.empty();
    }

    Map<String, Object> message = new HashMap<>();
    message.put("to", fcmToken);

    Map<String, String> notification = new HashMap<>();
    notification.put("title", title);
    notification.put("body", body);
    notification.put("sound", "default");
    notification.put("badge", "1");

    message.put("notification", notification);
    message.put("data", data);
    message.put("priority", "high");

    return webClient.post()
        .uri(fcmApiUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "key=" + fcmServerKey)
        .bodyValue(message)
        .retrieve()
        .bodyToMono(String.class)
        .doOnSuccess(response -> log.debug("Push notification sent successfully"))
        .doOnError(e -> log.error("Failed to send push notification", e))
        .then();
  }

  /**
   * 피싱 리포트 데이터 페이로드 생성
   */
  private Map<String, Object> createDataPayload(PhishingReport report) {
    Map<String, Object> data = new HashMap<>();
    data.put("type", "phishing_alert");
    data.put("reportId", report.getId().toString());
    data.put("sender", report.getSender());
    data.put("riskLevel", report.getRiskLevel());
    data.put("riskScore", report.getRiskScore());
    data.put("timestamp", report.getTimestamp().getTime());

    if (report.getLocation() != null
        && report.getLocation().getLatitude() != null
        && report.getLocation().getLongitude() != null) {
      Map<String, Double> location = new HashMap<>();
      location.put("latitude", report.getLocation().getLatitude());
      location.put("longitude", report.getLocation().getLongitude());
      data.put("location", location);
    }

    return data;
  }

  /**
   * 일괄 알림 전송 (배치) - 비동기 병렬 처리
   */
  @Async("notificationExecutor")
  public void sendBatchNotifications(List<String> userIds, String title, String body,
      Map<String, Object> data) {
    Set<ObjectId> objectIds = userIds.stream()
        .map(ObjectId::new)
        .collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(objectIds);

    Flux.fromIterable(users)
        .filter(user -> user.getFcmToken() != null && !user.getFcmToken().isEmpty())
        .flatMap(user -> sendPushNotificationAsync(user.getFcmToken(), title, body, data)
            .onErrorResume(e -> {
              log.error("Failed to send batch notification to user: {}", user.getId(), e);
              return Mono.empty();
            }))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
  }

  /**
   * 사일런트 푸시 전송 (백그라운드 동기화용)
   */
  public void sendSilentPush(String fcmToken, Map<String, Object> data) {
    if (fcmToken == null || fcmToken.isEmpty()) {
      return;
    }

    if (fcmServerKey == null || fcmServerKey.isEmpty()) {
      return;
    }

    Map<String, Object> message = new HashMap<>();
    message.put("to", fcmToken);
    message.put("data", data);
    message.put("priority", "high");
    message.put("content_available", true);  // iOS silent push

    webClient.post()
        .uri(fcmApiUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "key=" + fcmServerKey)
        .bodyValue(message)
        .retrieve()
        .bodyToMono(String.class)
        .doOnError(e -> log.error("Failed to send silent push", e))
        .subscribe();
  }
}
