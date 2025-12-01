package com.weave.domain.schedule.repository;

import com.weave.domain.schedule.entity.Schedule;
import java.util.Date;
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
  List<Schedule> findSchedulesStartingToday(Date startOfDay, Date endOfDay);

  /**
   * 특정 시간에 시작하는 일정 조회 (범위)
   */
  @Query("{ 'start_date': { $gte: ?0, $lte: ?1 } }")
  List<Schedule> findSchedulesStartingBetween(Date startTime, Date endTime);

  /**
   * 오늘 일정 조회
   */
  @Query("{ 'start_date': { $gte: ?0, $lt: ?1 } }")
  List<Schedule> findScheduleToday(Date startOfDay, Date endOfDay);

  /**
   * 워크스페이스별 일정 조회
   */
  List<Schedule> findByWorkspace(ObjectId workspaceId);

  /**
   * 참여자가 포함된 일정 조회
   */
  List<Schedule> findByParticipantsContaining(ObjectId userId);

  /**
   * 워크스페이스별 특정 기간 일정 조회 (DB 레벨 필터링으로 성능 최적화)
   */
  @Query("{ 'workspace': ?0, 'start_date': { $gte: ?1, $lt: ?2 } }")
  List<Schedule> findByWorkspaceAndDateRange(ObjectId workspaceId, Date startDate, Date endDate);

  /**
   * 워크스페이스별 특정 날짜 이후 일정 조회 (Feed용 최적화)
   */
  @Query("{ 'workspace': ?0, 'start_date': { $gte: ?1 }, 'participants': ?2 }")
  List<Schedule> findByWorkspaceAndStartDateAfterAndParticipant(
      ObjectId workspaceId, Date startDate, ObjectId participantId);

  /**
   * 워크스페이스별 연도 일정 조회
   */
  @Query("{ 'workspace': ?0, 'start_date': { $gte: ?1, $lt: ?2 } }")
  List<Schedule> findByWorkspaceAndYear(ObjectId workspaceId, Date yearStart, Date yearEnd);

  /**
   * 알림 대상 일정 조회 (reminderMinutes가 설정되어 있고, 아직 알림이 발송되지 않은 일정)
   * 현재 시간 + reminderMinutes 범위 내의 일정을 조회
   */
  @Query("{ 'reminder_minutes': { $ne: null }, 'reminder_sent': { $ne: true }, 'start_date': { $gte: ?0, $lt: ?1 } }")
  List<Schedule> findSchedulesForReminder(Date startTime, Date endTime);
}

