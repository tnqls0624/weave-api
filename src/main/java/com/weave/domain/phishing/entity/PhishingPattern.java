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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 피싱 패턴 엔티티
 * 피싱 탐지에 사용되는 패턴 규칙 저장
 */
@Document(collection = "phishing_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhishingPattern {

  @Id
  private ObjectId id;

  /**
   * 패턴 이름
   */
  @Field("name")
  @Indexed(unique = true)
  private String name;

  /**
   * 패턴 카테고리: financial, government, delivery, urgency, url, personal_info, event
   */
  @Field("category")
  @Indexed
  private String category;

  /**
   * 패턴 유형: regex, keyword, ml_feature
   */
  @Field("type")
  @Indexed
  private String type;

  /**
   * 패턴 정규식 또는 키워드 목록
   */
  @Field("patterns")
  private List<String> patterns;

  /**
   * 패턴 가중치 (0.0 ~ 1.0)
   */
  @Field("weight")
  private Double weight;

  /**
   * 패턴 설명
   */
  @Field("description")
  private String description;

  /**
   * 언어 코드 (ko, en, etc.)
   */
  @Field("language")
  @Indexed
  private String language;

  /**
   * 활성화 상태
   */
  @Field("is_active")
  @Indexed
  @Builder.Default
  private Boolean isActive = true;

  /**
   * 우선순위 (낮을수록 높은 우선순위)
   */
  @Field("priority")
  @Builder.Default
  private Integer priority = 100;

  /**
   * 매칭 횟수 (통계용)
   */
  @Field("match_count")
  @Builder.Default
  private Long matchCount = 0L;

  /**
   * 오탐지 횟수 (통계용)
   */
  @Field("false_positive_count")
  @Builder.Default
  private Long falsePositiveCount = 0L;

  /**
   * 정확도 (match_count / (match_count + false_positive_count))
   */
  @Field("accuracy")
  private Double accuracy;

  /**
   * 마지막 사용 시각
   */
  @Field("last_used_at")
  private Date lastUsedAt;

  /**
   * 생성자 (관리자 ID)
   */
  @Field("created_by")
  private ObjectId createdBy;

  /**
   * 수정자 (관리자 ID)
   */
  @Field("updated_by")
  private ObjectId updatedBy;

  @CreatedDate
  @Field("created_at")
  private Date createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private Date updatedAt;

  /**
   * 정확도 계산
   */
  public void updateAccuracy() {
    if (matchCount > 0 || falsePositiveCount > 0) {
      this.accuracy = matchCount.doubleValue() /
        (matchCount.doubleValue() + falsePositiveCount.doubleValue());
    }
  }
}