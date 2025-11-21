package com.weave.domain.phishing.service;

import com.weave.domain.phishing.dto.PhishingReportRequestDto;
import com.weave.domain.phishing.dto.PhishingReportResponseDto;
import com.weave.domain.phishing.dto.PhishingStatisticsDto;
import com.weave.domain.phishing.entity.PhishingReport;
import com.weave.domain.phishing.entity.PhishingStatistics;
import com.weave.domain.phishing.repository.PhishingReportRepository;
import com.weave.domain.phishing.repository.PhishingStatisticsRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피싱 가드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhishingGuardService {

  private final PhishingReportRepository phishingReportRepository;
  private final PhishingStatisticsRepository phishingStatisticsRepository;
  private final UserRepository userRepository;
  private final PhishingDetectionService detectionService;
  private final PhishingNotificationService notificationService;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 피싱 신고 접수
   */
  @Transactional
  public PhishingReportResponseDto reportPhishing(String userEmail, PhishingReportRequestDto dto) {
    log.info("피싱 신고 접수 - 사용자: {}, SMS ID: {}", userEmail, dto.getSmsId());

    // 중복 신고 체크
    if (phishingReportRepository.existsBySmsId(dto.getSmsId())) {
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
    }

    // 사용자 조회
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 엔티티 생성
    PhishingReport report = PhishingReport.builder()
        .smsId(dto.getSmsId())
        .userId(user.getId())
        .userEmail(user.getEmail())
        .sender(dto.getSender())
        .message(dto.getMessage())
        .riskScore(dto.getRiskScore())
        .riskLevel(dto.getRiskLevel())
        .detectionReasons(dto.getDetectionReasons())
        .phishingType(dto.getPhishingType())
        .status("pending")
        .autoBlocked(dto.getAutoBlocked() != null ? dto.getAutoBlocked() : false)
        .timestamp(new Date(dto.getTimestamp()))
        .build();

    // 위치 정보 설정
    if (dto.getLocation() != null) {
      report.setLocation(PhishingReport.Location.builder()
          .latitude(dto.getLocation().getLatitude())
          .longitude(dto.getLocation().getLongitude())
          .address(dto.getLocation().getAddress())
          .city(dto.getLocation().getCity())
          .country(dto.getLocation().getCountry())
          .build());
    }

    // 디바이스 정보 설정
    if (dto.getDeviceInfo() != null) {
      report.setDeviceInfo(PhishingReport.DeviceInfo.builder()
          .platform(dto.getDeviceInfo().getPlatform())
          .version(dto.getDeviceInfo().getVersion())
          .model(dto.getDeviceInfo().getModel())
          .manufacturer(dto.getDeviceInfo().getManufacturer())
          .osVersion(dto.getDeviceInfo().getOsVersion())
          .build());
    }

    // 워크스페이스 설정
    if (dto.getWorkspaceId() != null) {
      report.setWorkspaceId(new ObjectId(dto.getWorkspaceId()));
    }

    // 저장
    PhishingReport savedReport = phishingReportRepository.save(report);

    // 통계 업데이트
    updateStatistics(savedReport);

    // 알림 처리
    String body = "발신자: " + savedReport.getSender() + ", 위험 수준: " + savedReport.getRiskLevel();

    processNotifications(savedReport, body);

    // WebSocket으로 실시간 브로드캐스트
    broadcastPhishingAlert(savedReport);

    return PhishingReportResponseDto.from(savedReport);
  }

  /**
   * 피싱 신고 목록 조회
   */
  public Page<PhishingReportResponseDto> getReports(String userEmail, Pageable pageable) {
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    Page<PhishingReport> reports = phishingReportRepository
        .findByUserIdOrderByTimestampDesc(user.getId(), pageable);

    return reports.map(PhishingReportResponseDto::from);
  }

  /**
   * 워크스페이스별 피싱 신고 조회
   */
  public Page<PhishingReportResponseDto> getWorkspaceReports(String workspaceId,
      Pageable pageable) {
    ObjectId wsId = new ObjectId(workspaceId);
    Page<PhishingReport> reports = phishingReportRepository
        .findByWorkspaceIdOrderByTimestampDesc(wsId, pageable);

    return reports.map(PhishingReportResponseDto::from);
  }

  /**
   * 피싱 신고 상세 조회
   */
  public PhishingReportResponseDto getReport(String reportId) {
    PhishingReport report = phishingReportRepository.findById(new ObjectId(reportId))
        .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));

    return PhishingReportResponseDto.from(report);
  }

  /**
   * 피싱 신고 상태 업데이트
   */
  @Transactional
  public PhishingReportResponseDto updateReportStatus(String reportId, String status,
      String adminNote) {
    PhishingReport report = phishingReportRepository.findById(new ObjectId(reportId))
        .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));

    report.setStatus(status);
    if (adminNote != null) {
      report.setAdminNote(adminNote);
    }

    if ("verified".equals(status)) {
      report.setVerifiedAt(new Date());
    }

    PhishingReport updated = phishingReportRepository.save(report);
    return PhishingReportResponseDto.from(updated);
  }

  /**
   * 사용자 피드백 추가
   */
  @Transactional
  public PhishingReportResponseDto addUserFeedback(String reportId, String userEmail,
      String feedback) {
    PhishingReport report = phishingReportRepository.findById(new ObjectId(reportId))
        .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));

    // 권한 확인
    if (!report.getUserEmail().equals(userEmail)) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }

    report.setUserFeedback(feedback);

    // False positive 처리
    if (feedback.toLowerCase().contains("false") || feedback.toLowerCase().contains("오탐")) {
      report.setStatus("false_positive");
      updateFalsePositiveStatistics(report);
    }

    PhishingReport updated = phishingReportRepository.save(report);
    return PhishingReportResponseDto.from(updated);
  }

  /**
   * 피싱 통계 조회
   */
  public PhishingStatisticsDto getStatistics(String userEmail) {
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 오늘 날짜
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();

    // 오늘 통계 조회 또는 생성
    PhishingStatistics stats = phishingStatisticsRepository
        .findByUserIdAndDateAndStatType(user.getId(), todayStr, "daily")
        .orElseGet(() -> {
          PhishingStatistics newStats = PhishingStatistics.builder()
              .userId(user.getId())
              .date(todayStr)
              .statType("daily")
              .totalScanned(0L)
              .phishingDetected(0L)
              .highRiskCount(0L)
              .mediumRiskCount(0L)
              .lowRiskCount(0L)
              .autoBlockedCount(0L)
              .falsePositiveCount(0L)
              .avgRiskScore(0.0)
              .detectionRate(0.0)
              .accuracyRate(0.0)
              .build();
          // DB에 저장하여 createdAt, updatedAt 자동 생성
          return phishingStatisticsRepository.save(newStats);
        });

    return PhishingStatisticsDto.from(stats);
  }

  /**
   * 워크스페이스 피싱 통계 조회
   */
  public PhishingStatisticsDto getWorkspaceStatistics(String workspaceId) {
    ObjectId wsId = new ObjectId(workspaceId);

    // 오늘 날짜
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();

    // 워크스페이스 통계 조회
    PhishingStatistics stats = phishingStatisticsRepository
        .findByWorkspaceIdAndDateAndStatType(wsId, todayStr, "daily")
        .orElseGet(() -> {
          PhishingStatistics newStats = PhishingStatistics.builder()
              .workspaceId(wsId)
              .date(todayStr)
              .statType("daily")
              .totalScanned(0L)
              .phishingDetected(0L)
              .highRiskCount(0L)
              .mediumRiskCount(0L)
              .lowRiskCount(0L)
              .autoBlockedCount(0L)
              .falsePositiveCount(0L)
              .avgRiskScore(0.0)
              .detectionRate(0.0)
              .accuracyRate(0.0)
              .build();
          // DB에 저장하여 createdAt, updatedAt 자동 생성
          return phishingStatisticsRepository.save(newStats);
        });

    return PhishingStatisticsDto.from(stats);
  }

  /**
   * 근처 피싱 알림 조회
   */
  public List<PhishingReportResponseDto> getNearbyReports(double latitude, double longitude,
      double radius) {
    // 위도/경도 범위 계산 (1도 ≈ 111km)
    double latDelta = radius / 111000.0; // 미터를 도(degree)로 변환
    double lonDelta = radius / 111000.0;

    double minLat = latitude - latDelta;
    double maxLat = latitude + latDelta;
    double minLon = longitude - lonDelta;
    double maxLon = longitude + lonDelta;

    // MongoDB 지리 공간 쿼리 사용
    List<PhishingReport> nearbyReports = phishingReportRepository
        .findNearbyReports(minLat, maxLat, minLon, maxLon);

    return nearbyReports.stream()
        .map(PhishingReportResponseDto::from)
        .collect(Collectors.toList());
  }

  /**
   * 고위험 미처리 신고 조회 (관리자용)
   */
  public List<PhishingReportResponseDto> getHighRiskPendingReports() {
    List<PhishingReport> reports = phishingReportRepository.findHighRiskPendingReports();
    return reports.stream()
        .map(PhishingReportResponseDto::from)
        .collect(Collectors.toList());
  }

  /**
   * 통계 업데이트
   */
  private void updateStatistics(PhishingReport report) {
    try {
      LocalDate today = LocalDate.now();
      String todayStr = today.toString();

      // 사용자 통계 업데이트
      PhishingStatistics userStats = phishingStatisticsRepository
          .findByUserIdAndDateAndStatType(report.getUserId(), todayStr, "daily")
          .orElseGet(() -> PhishingStatistics.builder()
              .userId(report.getUserId())
              .date(todayStr)
              .statType("daily")
              .build());

      userStats.setPhishingDetected(userStats.getPhishingDetected() + 1);

      // 위험 수준별 카운트
      switch (report.getRiskLevel()) {
        case "high":
          userStats.setHighRiskCount(userStats.getHighRiskCount() + 1);
          break;
        case "medium":
          userStats.setMediumRiskCount(userStats.getMediumRiskCount() + 1);
          break;
        case "low":
          userStats.setLowRiskCount(userStats.getLowRiskCount() + 1);
          break;
      }

      if (report.getAutoBlocked()) {
        userStats.setAutoBlockedCount(userStats.getAutoBlockedCount() + 1);
      }

      // 피싱 유형 통계
      if (report.getPhishingType() != null) {
        userStats.addPhishingType(report.getPhishingType());
      }

      // 발신자 통계
      userStats.addSender(report.getSender());

      // 시간대 통계
      int hour = java.time.LocalDateTime.now().getHour();
      userStats.addHourlyCount(hour);

      userStats.updateRates();
      phishingStatisticsRepository.save(userStats);

      // 워크스페이스 통계도 업데이트
      if (report.getWorkspaceId() != null) {
        updateWorkspaceStatistics(report);
      }

    } catch (Exception e) {
      log.error("통계 업데이트 실패", e);
    }
  }

  /**
   * 워크스페이스 통계 업데이트
   */
  private void updateWorkspaceStatistics(PhishingReport report) {
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();

    PhishingStatistics wsStats = phishingStatisticsRepository
        .findByWorkspaceIdAndDateAndStatType(report.getWorkspaceId(), todayStr, "daily")
        .orElseGet(() -> PhishingStatistics.builder()
            .workspaceId(report.getWorkspaceId())
            .date(todayStr)
            .statType("daily")
            .build());

    wsStats.setPhishingDetected(wsStats.getPhishingDetected() + 1);

    // 위험 수준별 카운트
    switch (report.getRiskLevel()) {
      case "high":
        wsStats.setHighRiskCount(wsStats.getHighRiskCount() + 1);
        break;
      case "medium":
        wsStats.setMediumRiskCount(wsStats.getMediumRiskCount() + 1);
        break;
      case "low":
        wsStats.setLowRiskCount(wsStats.getLowRiskCount() + 1);
        break;
    }

    wsStats.updateRates();
    phishingStatisticsRepository.save(wsStats);
  }

  /**
   * 오탐지 통계 업데이트
   */
  private void updateFalsePositiveStatistics(PhishingReport report) {
    try {
      LocalDate today = LocalDate.now();
      String todayStr = today.toString();

      // 사용자 통계에서 오탐지 수 증가
      PhishingStatistics userStats = phishingStatisticsRepository
          .findByUserIdAndDateAndStatType(report.getUserId(), todayStr, "daily")
          .orElse(null);

      if (userStats != null) {
        userStats.setFalsePositiveCount(userStats.getFalsePositiveCount() + 1);
        userStats.updateRates();
        phishingStatisticsRepository.save(userStats);
      }

      // 패턴 정확도 업데이트
      detectionService.updatePatternAccuracy(report.getDetectionReasons(), false);

    } catch (Exception e) {
      log.error("오탐지 통계 업데이트 실패", e);
    }
  }

  /**
   * 알림 처리
   */
  private void processNotifications(PhishingReport report, String body) {
    try {
      // 고위험 알림
      if ("high".equals(report.getRiskLevel())) {
        notificationService.sendHighRiskAlert(report);
      }

      // 워크스페이스 멤버에게 알림
      if (report.getWorkspaceId() != null) {
        notificationService.notifyWorkspaceMembers(report.getWorkspaceId(), "새로운 피싱 신고 접수", body,
            report);
      }

    } catch (Exception e) {
      log.error("알림 처리 실패", e);
    }
  }

  /**
   * WebSocket으로 실시간 알림 브로드캐스트
   */
  private void broadcastPhishingAlert(PhishingReport report) {
    try {
      PhishingReportResponseDto dto = PhishingReportResponseDto.from(report);

      // 전체 피싱 알림 토픽
      messagingTemplate.convertAndSend("/topic/phishing.alerts", dto);

      // 워크스페이스별 알림
      if (report.getWorkspaceId() != null) {
        messagingTemplate.convertAndSend(
            "/topic/phishing.alerts." + report.getWorkspaceId(),
            dto
        );
      }

      // 개인 알림
      messagingTemplate.convertAndSendToUser(
          report.getUserEmail(),
          "/queue/phishing.personal",
          dto
      );

      log.info("피싱 알림 브로드캐스트 완료 - SMS ID: {}", report.getSmsId());

    } catch (Exception e) {
      log.error("피싱 알림 브로드캐스트 실패", e);
    }
  }
}