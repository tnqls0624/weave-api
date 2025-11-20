package com.weave.domain.phishing.dto;

import com.weave.domain.phishing.entity.PhishingStatistics;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 피싱 통계 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhishingStatisticsDto {

  private String id;
  private String statType;
  private String date;
  private String userId;
  private String workspaceId;
  private Long totalScanned;
  private Long phishingDetected;
  private Long highRiskCount;
  private Long mediumRiskCount;
  private Long lowRiskCount;
  private Long autoBlockedCount;
  private Long falsePositiveCount;
  private Map<String, Long> phishingTypeStats;
  private Map<String, Long> topSenders;
  private Map<Integer, Long> hourlyStats;
  private Double avgRiskScore;
  private Double detectionRate;
  private Double accuracyRate;
  private Date createdAt;
  private Date updatedAt;

  /**
   * Entity to DTO 변환
   */
  public static PhishingStatisticsDto from(PhishingStatistics stats) {
    if (stats == null) {
      return PhishingStatisticsDto.builder()
        .totalScanned(0L)
        .phishingDetected(0L)
        .highRiskCount(0L)
        .mediumRiskCount(0L)
        .lowRiskCount(0L)
        .autoBlockedCount(0L)
        .falsePositiveCount(0L)
        .phishingTypeStats(new HashMap<>())
        .topSenders(new HashMap<>())
        .hourlyStats(new HashMap<>())
        .detectionRate(0.0)
        .accuracyRate(0.0)
        .build();
    }

    return PhishingStatisticsDto.builder()
      .id(stats.getId() != null ? stats.getId().toString() : null)
      .statType(stats.getStatType())
      .date(stats.getDate())
      .userId(stats.getUserId() != null ? stats.getUserId().toString() : null)
      .workspaceId(stats.getWorkspaceId() != null ? stats.getWorkspaceId().toString() : null)
      .totalScanned(stats.getTotalScanned() != null ? stats.getTotalScanned() : 0L)
      .phishingDetected(stats.getPhishingDetected() != null ? stats.getPhishingDetected() : 0L)
      .highRiskCount(stats.getHighRiskCount() != null ? stats.getHighRiskCount() : 0L)
      .mediumRiskCount(stats.getMediumRiskCount() != null ? stats.getMediumRiskCount() : 0L)
      .lowRiskCount(stats.getLowRiskCount() != null ? stats.getLowRiskCount() : 0L)
      .autoBlockedCount(stats.getAutoBlockedCount() != null ? stats.getAutoBlockedCount() : 0L)
      .falsePositiveCount(stats.getFalsePositiveCount() != null ? stats.getFalsePositiveCount() : 0L)
      .phishingTypeStats(stats.getPhishingTypeStats() != null ? stats.getPhishingTypeStats() : new HashMap<>())
      .topSenders(stats.getTopSenders() != null ? stats.getTopSenders() : new HashMap<>())
      .hourlyStats(stats.getHourlyStats() != null ? stats.getHourlyStats() : new HashMap<>())
      .avgRiskScore(stats.getAvgRiskScore() != null ? stats.getAvgRiskScore() : 0.0)
      .detectionRate(stats.getDetectionRate() != null ? stats.getDetectionRate() : 0.0)
      .accuracyRate(stats.getAccuracyRate() != null ? stats.getAccuracyRate() : 0.0)
      .createdAt(stats.getCreatedAt())
      .updatedAt(stats.getUpdatedAt())
      .build();
  }
}