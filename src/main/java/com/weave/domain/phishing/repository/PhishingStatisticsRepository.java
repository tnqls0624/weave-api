package com.weave.domain.phishing.repository;

import com.weave.domain.phishing.entity.PhishingStatistics;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 피싱 통계 리포지토리
 */
@Repository
public interface PhishingStatisticsRepository extends MongoRepository<PhishingStatistics, ObjectId> {

  /**
   * 사용자별 날짜별 통계 조회
   */
  Optional<PhishingStatistics> findByUserIdAndDateAndStatType(ObjectId userId, String date, String statType);

  /**
   * 워크스페이스별 날짜별 통계 조회
   */
  Optional<PhishingStatistics> findByWorkspaceIdAndDateAndStatType(ObjectId workspaceId, String date, String statType);

  /**
   * 사용자별 기간 통계 조회
   */
  @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'statType': ?3 }")
  List<PhishingStatistics> findUserStatsByDateRange(ObjectId userId, String startDate, String endDate, String statType);

  /**
   * 워크스페이스별 기간 통계 조회
   */
  @Query("{ 'workspaceId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'statType': ?3 }")
  List<PhishingStatistics> findWorkspaceStatsByDateRange(ObjectId workspaceId, String startDate, String endDate, String statType);

  /**
   * 전체 통계 조회 (특정 날짜)
   */
  @Query("{ 'date': ?0, 'statType': ?1, 'userId': null, 'workspaceId': null }")
  Optional<PhishingStatistics> findGlobalStatsByDate(String date, String statType);

  /**
   * 사용자별 최근 통계
   */
  List<PhishingStatistics> findByUserIdOrderByDateDesc(ObjectId userId);

  /**
   * 워크스페이스별 최근 통계
   */
  List<PhishingStatistics> findByWorkspaceIdOrderByDateDesc(ObjectId workspaceId);
}