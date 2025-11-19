package com.weave.domain.phishing.entity;

import java.util.Date;
import java.util.List;
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
 * 피싱 신고 엔티티
 * SMS 피싱 감지 및 신고 정보를 저장
 */
@Document(collection = "phishing_reports")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_timestamp", def = "{'userId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_workspace_risk", def = "{'workspaceId': 1, 'riskLevel': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhishingReport {

  @Id
  private ObjectId id;

  /**
   * SMS 고유 ID (클라이언트에서 생성)
   */
  @Field("sms_id")
  @Indexed(unique = true)
  private String smsId;

  /**
   * 신고한 사용자 ID
   */
  @Field("user_id")
  @Indexed
  private ObjectId userId;

  /**
   * 사용자 이메일 (빠른 조회용)
   */
  @Field("user_email")
  @Indexed
  private String userEmail;

  /**
   * 워크스페이스 ID (선택사항)
   */
  @Field("workspace_id")
  @Indexed
  private ObjectId workspaceId;

  /**
   * SMS 발신자 번호/ID
   */
  @Field("sender")
  @Indexed
  private String sender;

  /**
   * SMS 메시지 내용
   */
  @Field("message")
  private String message;

  /**
   * 위험도 점수 (0.0 ~ 1.0)
   */
  @Field("risk_score")
  @Indexed
  private Double riskScore;

  /**
   * 위험 수준: high, medium, low
   */
  @Field("risk_level")
  @Indexed
  private String riskLevel;

  /**
   * 탐지 이유 목록
   */
  @Field("detection_reasons")
  private List<String> detectionReasons;

  /**
   * 피싱 유형 분류
   */
  @Field("phishing_type")
  private String phishingType;

  /**
   * 위치 정보 (선택사항)
   */
  @Field("location")
  private Location location;

  /**
   * 디바이스 정보
   */
  @Field("device_info")
  private DeviceInfo deviceInfo;

  /**
   * 처리 상태: pending, verified, false_positive, blocked
   */
  @Field("status")
  @Indexed
  @Builder.Default
  private String status = "pending";

  /**
   * 자동 차단 여부
   */
  @Field("auto_blocked")
  @Builder.Default
  private Boolean autoBlocked = false;

  /**
   * 사용자 피드백
   */
  @Field("user_feedback")
  private String userFeedback;

  /**
   * 관리자 노트
   */
  @Field("admin_note")
  private String adminNote;

  /**
   * 신고 시각
   */
  @Field("timestamp")
  @Indexed
  private Date timestamp;

  /**
   * 검증 시각
   */
  @Field("verified_at")
  private Date verifiedAt;

  @CreatedDate
  @Field("created_at")
  private Date createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private Date updatedAt;

  /**
   * 위치 정보 내부 클래스
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Location {
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String country;
  }

  /**
   * 디바이스 정보 내부 클래스
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DeviceInfo {
    private String platform;
    private String version;
    private String model;
    private String manufacturer;
    private String osVersion;
  }
}