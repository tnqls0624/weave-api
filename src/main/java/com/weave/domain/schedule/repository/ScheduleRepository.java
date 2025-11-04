package com.weave.domain.schedule.repository;

import com.weave.domain.schedule.entity.Schedule;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends MongoRepository<Schedule, ObjectId> {

  /**
   * 오늘 시작하는 일정 조회
   */
  @Query("{ 'start_date': { $gte: ?0, $lt: ?1 } }")
  List<Schedule> findSchedulesStartingToday(String startOfDay, String endOfDay);

  default List<Schedule> findSchedulesStartingToday(String todayStr) {
    String startOfDay = todayStr.substring(0, 10) + " 00:00:00";
    String endOfDay = todayStr.substring(0, 10) + " 23:59:59";
    return findSchedulesStartingToday(startOfDay, endOfDay);
  }

  /**
   * 특정 시간에 시작하는 일정 조회 (±5분 범위)
   */
  @Query("{ 'start_date': { $gte: ?0, $lte: ?1 } }")
  List<Schedule> findSchedulesStartingBetween(String startTime, String endTime);

  default List<Schedule> findSchedulesStartingAt(String targetTime) {
    // ±5분 범위로 검색
    return findSchedulesStartingBetween(targetTime, targetTime);
  }

  /**
   * 오늘 일정 조회
   */
  @Query("{ 'start_date': { $gte: ?0, $lt: ?1 } }")
  List<Schedule> findScheduleToday(String startOfDay, String endOfDay);

  default List<Schedule> findScheduleToday(String todayStr) {
    String startOfDay = todayStr.substring(0, 10) + " 00:00:00";
    String endOfDay = todayStr.substring(0, 10) + " 23:59:59";
    return findScheduleToday(startOfDay, endOfDay);
  }

  /**
   * 워크스페이스별 일정 조회
   */
  List<Schedule> findByWorkspace(ObjectId workspaceId);

  /**
   * 참여자가 포함된 일정 조회
   */
  List<Schedule> findByParticipantsContaining(ObjectId userId);
}

