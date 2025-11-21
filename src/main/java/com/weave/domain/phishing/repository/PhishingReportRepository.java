package com.weave.domain.phishing.repository;

import com.weave.domain.phishing.entity.PhishingReport;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 피싱 신고 리포지토리
 */
@Repository
public interface PhishingReportRepository extends MongoRepository<PhishingReport, ObjectId> {

  /**
   * SMS ID로 조회
   */
  Optional<PhishingReport> findBySmsId(String smsId);

  /**
   * 사용자 ID로 조회
   */
  Page<PhishingReport> findByUserIdOrderByTimestampDesc(ObjectId userId, Pageable pageable);

  /**
   * 워크스페이스 ID로 조회
   */
  Page<PhishingReport> findByWorkspaceIdOrderByTimestampDesc(ObjectId workspaceId,
      Pageable pageable);

  /**
   * 사용자 이메일로 조회
   */
  List<PhishingReport> findByUserEmailOrderByTimestampDesc(String userEmail);

  /**
   * 위험 수준으로 조회
   */
  List<PhishingReport> findByRiskLevelOrderByTimestampDesc(String riskLevel);

  /**
   * 발신자로 조회
   */
  List<PhishingReport> findBySenderOrderByTimestampDesc(String sender);

  /**
   * 상태로 조회
   */
  List<PhishingReport> findByStatusOrderByTimestampDesc(String status);

  /**
   * 기간별 조회
   */
  List<PhishingReport> findByTimestampBetweenOrderByTimestampDesc(Date start, Date end);

  /**
   * 워크스페이스별 기간 조회
   */
  @Query("{ 'workspaceId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
  List<PhishingReport> findByWorkspaceAndTimestamp(ObjectId workspaceId, Date start, Date end);

  /**
   * 위치 기반 조회 (근처 피싱 알림) 주의: GeoJSON이 아닌 일반 좌표 사용을 위한 범위 쿼리 실제 거리는 대략적인 위경도 차이로 계산 (1도 ≈ 111km)
   */
  @Query("{ 'location': { $exists: true, $ne: null }, " +
      "'location.latitude': { $gte: ?0 - (?2 / 111000), $lte: ?0 + (?2 / 111000) }, " +
      "'location.longitude': { $gte: ?1 - (?2 / 111000), $lte: ?1 + (?2 / 111000) } }")
  List<PhishingReport> findNearbyReports(double latitude, double longitude, double maxDistance);

  /**
   * 고위험 미처리 건 조회
   */
  @Query("{ 'riskLevel': 'high', 'status': 'pending' }")
  List<PhishingReport> findHighRiskPendingReports();

  /**
   * 자동 차단된 건 조회
   */
  List<PhishingReport> findByAutoBlockedTrue();

  /**
   * 워크스페이스별 통계용 카운트
   */
  long countByWorkspaceIdAndRiskLevel(ObjectId workspaceId, String riskLevel);

  /**
   * 사용자별 통계용 카운트
   */
  long countByUserIdAndRiskLevel(ObjectId userId, String riskLevel);

  /**
   * 기간별 피싱 수 카운트
   */
  long countByTimestampBetween(Date start, Date end);

  /**
   * SMS ID 중복 체크
   */
  boolean existsBySmsId(String smsId);

  /**
   * 발신자별 최근 피싱 보고 조회
   */
  @Query(value = "{ 'sender': ?0 }", sort = "{ 'timestamp': -1 }")
  Page<PhishingReport> findRecentBySender(String sender, Pageable pageable);

  /**
   * 오늘 신고된 피싱 조회
   */
  @Query("{ 'timestamp': { $gte: ?0 } }")
  List<PhishingReport> findTodayReports(Date todayStart);

  /**
   * 미검증 피싱 신고 조회
   */
  @Query("{ 'status': 'pending', 'verifiedAt': null }")
  Page<PhishingReport> findUnverifiedReports(Pageable pageable);

  /**
   * 사용자 피드백이 있는 신고 조회
   */
  @Query("{ 'userFeedback': { $exists: true, $ne: null } }")
  List<PhishingReport> findReportsWithFeedback();

  /**
   * 사용자별 최근 피싱 조회 (페이징)
   */
  List<PhishingReport> findByUserId(ObjectId userId, PageRequest pageable);

  /**
   * 워크스페이스별 위험도별 피싱 조회
   */
  List<PhishingReport> findByWorkspaceIdAndRiskLevel(ObjectId workspaceId, String riskLevel,
      PageRequest pageable);

  /**
   * 워크스페이스별 기간별 피싱 조회
   */
  @Query("{ 'workspaceId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
  List<PhishingReport> findByWorkspaceIdAndDateRange(ObjectId workspaceId, Date startDate,
      Date endDate);

  List<PhishingReport> findByUserEmail(String userEmail);
}