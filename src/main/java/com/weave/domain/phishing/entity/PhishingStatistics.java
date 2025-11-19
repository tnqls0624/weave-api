package com.weave.domain.phishing.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 피싱 통계 엔티티 사용자/워크스페이스별 피싱 통계 정보
 */
@Document(collection = "phishing_statistics")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_date", def = "{'userId': 1, 'date': -1}"),
    @CompoundIndex(name = "idx_workspace_date", def = "{'workspaceId': 1, 'date': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhishingStatistics {

  @Id
  private ObjectId id;

  /**
   * 통계 타입: daily, weekly, monthly, total
   */
  @Field("stat_type")
  @Indexed
  private String statType;

  /**
   * 통계 날짜 (daily: YYYY-MM-DD, weekly: YYYY-WW, monthly: YYYY-MM)
   */
  @Field("date")
  @Indexed
  private String date;

  /**
   * 사용자 ID (사용자별 통계인 경우)
   */
  @Field("user_id")
  @Indexed
  private ObjectId userId;

  /**
   * 워크스페이스 ID (워크스페이스별 통계인 경우)
   */
  @Field("workspace_id")
  @Indexed
  private ObjectId workspaceId;

  /**
   * 전체 스캔 메시지 수
   */
  @Field("total_scanned")
  @Builder.Default
  private Long totalScanned = 0L;

  /**
   * 피싱 감지 수
   */
  @Field("phishing_detected")
  @Builder.Default
  private Long phishingDetected = 0L;

  /**
   * 고위험 수
   */
  @Field("high_risk_count")
  @Builder.Default
  private Long highRiskCount = 0L;

  /**
   * 중위험 수
   */
  @Field("medium_risk_count")
  @Builder.Default
  private Long mediumRiskCount = 0L;

  /**
   * 저위험 수
   */
  @Field("low_risk_count")
  @Builder.Default
  private Long lowRiskCount = 0L;

  /**
   * 자동 차단 수
   */
  @Field("auto_blocked_count")
  @Builder.Default
  private Long autoBlockedCount = 0L;

  /**
   * 오탐지 수 (사용자가 false positive로 표시)
   */
  @Field("false_positive_count")
  @Builder.Default
  private Long falsePositiveCount = 0L;

  /**
   * 피싱 유형별 통계
   */
  @Field("phishing_type_stats")
  @Builder.Default
  private Map<String, Long> phishingTypeStats = new HashMap<>();

  /**
   * 발신자별 통계 (상위 10개)
   */
  @Field("top_senders")
  @Builder.Default
  private Map<String, Long> topSenders = new HashMap<>();

  /**
   * 시간대별 통계 (0-23시)
   */
  @Field("hourly_stats")
  @Builder.Default
  private Map<Integer, Long> hourlyStats = new HashMap<>();

  /**
   * 평균 위험 점수
   */
  @Field("avg_risk_score")
  private Double avgRiskScore;

  /**
   * 탐지율 (phishing_detected / total_scanned)
   */
  @Field("detection_rate")
  private Double detectionRate;

  /**
   * 정확도 (verified / phishing_detected)
   */
  @Field("accuracy_rate")
  private Double accuracyRate;

  @CreatedDate
  @Field("created_at")
  private Date createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private Date updatedAt;

  /**
   * 통계 업데이트 메서드
   */
  public void updateRates() {
    // 탐지율 계산
    if (totalScanned > 0) {
      this.detectionRate = phishingDetected.doubleValue() / totalScanned.doubleValue();
    }

    // 정확도 계산
    if (phishingDetected > 0 && falsePositiveCount >= 0) {
      this.accuracyRate = (phishingDetected - falsePositiveCount) /
          phishingDetected.doubleValue();
    }
  }

  /**
   * 피싱 유형 통계 추가
   */
  public void addPhishingType(String type) {
    phishingTypeStats.merge(type, 1L, Long::sum);
  }

  /**
   * 발신자 통계 추가
   */
  public void addSender(String sender) {
    topSenders.merge(sender, 1L, Long::sum);

    // 상위 10개만 유지
    if (topSenders.size() > 10) {
      String minSender = topSenders.entrySet().stream()
          .min(Map.Entry.comparingByValue())
          .map(Map.Entry::getKey)
          .orElse(null);
      if (minSender != null) {
        topSenders.remove(minSender);
      }
    }
  }

  /**
   * 시간대 통계 추가
   */
  public void addHourlyCount(int hour) {
    hourlyStats.merge(hour, 1L, Long::sum);
  }
}