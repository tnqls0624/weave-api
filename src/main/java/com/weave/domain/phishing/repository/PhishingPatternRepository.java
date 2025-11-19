package com.weave.domain.phishing.repository;

import com.weave.domain.phishing.entity.PhishingPattern;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 피싱 패턴 리포지토리
 */
@Repository
public interface PhishingPatternRepository extends MongoRepository<PhishingPattern, ObjectId> {

  /**
   * 활성화된 패턴 조회
   */
  List<PhishingPattern> findByIsActiveTrue();

  /**
   * 카테고리별 패턴 조회
   */
  List<PhishingPattern> findByCategoryAndIsActive(String category, boolean isActive);

  /**
   * 언어별 패턴 조회
   */
  List<PhishingPattern> findByLanguageAndIsActive(String language, boolean isActive);

  /**
   * 카테고리와 언어로 패턴 조회
   */
  List<PhishingPattern> findByCategoryAndLanguageAndIsActive(String category, String language, boolean isActive);

  /**
   * 설명으로 패턴 조회 (정확도 업데이트용)
   */
  List<PhishingPattern> findByDescriptionIn(List<String> descriptions);

  /**
   * 최소 정확도 이상 패턴 조회
   */
  @Query("{ 'accuracy': { $gte: ?0 }, 'isActive': true }")
  List<PhishingPattern> findByMinAccuracy(double minAccuracy);

  /**
   * 최근 사용된 패턴 조회
   */
  List<PhishingPattern> findByIsActiveTrueOrderByLastUsedAtDesc();

  /**
   * 사용 빈도 높은 패턴 조회
   */
  List<PhishingPattern> findByIsActiveTrueOrderByMatchCountDesc();

  /**
   * 위험도별 패턴 조회
   */
  @Query("{ 'riskLevel': ?0, 'isActive': true }")
  List<PhishingPattern> findByRiskLevel(String riskLevel);

  /**
   * 특정 타입의 패턴 조회
   */
  List<PhishingPattern> findByTypeAndIsActive(String type, boolean isActive);

  /**
   * 오탐(False Positive) 비율이 높은 패턴 조회
   */
  @Query("{ 'falsePositiveCount': { $gte: ?0 }, 'isActive': true }")
  List<PhishingPattern> findHighFalsePositivePatterns(int threshold);

  /**
   * 패턴 이름으로 중복 체크
   */
  boolean existsByName(String name);

  /**
   * 생성자별 패턴 조회
   */
  List<PhishingPattern> findByCreatedBy(String createdBy);
}