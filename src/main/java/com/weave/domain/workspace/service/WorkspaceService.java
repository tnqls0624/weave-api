package com.weave.domain.workspace.service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.weave.domain.schedule.dto.HolidayDto;
import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.schedule.service.HolidayService;
import com.weave.domain.user.dto.UserResponseDto;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.domain.workspace.dto.CreateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.UpdateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.WorkspaceResponseDto;
import com.weave.domain.workspace.dto.WorkspaceScheduleItemDto;
import com.weave.domain.workspace.dto.WorkspaceScheduleResponseDto;
import com.weave.domain.workspace.entity.Workspace;
import com.weave.domain.workspace.repository.WorkspaceRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceService {

  private static final DateTimeFormatter HOLIDAY_DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyyMMdd");

  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;
  private final ScheduleRepository scheduleRepository;
  private final HolidayService holidayService;

  public WorkspaceResponseDto create(CreateWorkspaceRequestDto dto, String email) {

    User master = userRepository.findByEmail(email).orElseThrow(
        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
    );

    List<ObjectId> users = ImmutableList.of(master.getId());

    // 워크스페이스 생성 및 저장
    Workspace workspace = workspaceRepository.save(
        Workspace.builder()
            .master(master.getId())
            .users(users)
            .loveDay(dto.getLoveDay())
            .build()
    );
    // join master/users
    Map<ObjectId, User> uMap = loadUsersForWorkspace(workspace);
    return toDto(workspace, uMap);
  }

  public WorkspaceResponseDto update(UpdateWorkspaceRequestDto dto, String id) {
    Workspace workspace = workspaceRepository.findById(new ObjectId(id)).orElseThrow(
        () -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)
    );

    workspace.setLoveDay(dto.getLoveDay());
    workspaceRepository.save(workspace);

    Map<ObjectId, User> uMap = loadUsersForWorkspace(workspace);
    return toDto(workspace, uMap);
  }

  public WorkspaceResponseDto findById(String id) {
    Workspace workspace = workspaceRepository.findById(new ObjectId(id)).orElseThrow(
        () -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)
    );
    Map<ObjectId, User> uMap = loadUsersForWorkspace(workspace);
    return toDto(workspace, uMap);
  }

  public WorkspaceResponseDto[] find(String email) {
    log.info("find workspaces by email: {}", email);
    User user = userRepository.findByEmail(email).orElseThrow(
        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
    );

    List<Workspace> workspaces = workspaceRepository.findByUsersContaining(user.getId());
    Map<ObjectId, User> uMap = loadUsersForWorkspaces(workspaces);
    return workspaces.stream().map(ws -> toDto(ws, uMap)).toArray(WorkspaceResponseDto[]::new);
  }

  public void delete(String id) {
    workspaceRepository.deleteById(new ObjectId(id));
  }

  /**
   * 초대코드로 워크스페이스에 참여
   * - 초대코드로 대상 사용자 찾기
   * - 대상 사용자가 속한 워크스페이스 찾기
   * - 현재 사용자를 해당 워크스페이스에 추가
   */
  public WorkspaceResponseDto joinByInviteCode(String inviteCode, String email) {
    // 현재 사용자 조회
    User currentUser = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 초대코드로 대상 사용자 찾기
    User targetUser = userRepository.findByInviteCode(inviteCode)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

    // 자기 자신의 초대코드인 경우
    if (currentUser.getId().equals(targetUser.getId())) {
      throw new BusinessException(ErrorCode.CANNOT_INVITE_SELF);
    }

    // 대상 사용자가 master인 워크스페이스 찾기
    List<Workspace> targetWorkspaces = workspaceRepository.findByUsersContaining(targetUser.getId());
    Workspace workspace = targetWorkspaces.stream()
        .filter(ws -> ws.getMaster().equals(targetUser.getId()))
        .findFirst()
        .orElse(null);

    // 워크스페이스가 없으면 새로 생성
    if (workspace == null) {
      workspace = workspaceRepository.save(
          Workspace.builder()
              .master(targetUser.getId())
              .users(Lists.newArrayList(targetUser.getId(), currentUser.getId()))
              .build()
      );
    } else {
      // 이미 참여한 워크스페이스인지 확인
      if (workspace.getUsers().contains(currentUser.getId())) {
        throw new BusinessException(ErrorCode.ALREADY_JOINED_WORKSPACE);
      }
      // 워크스페이스에 현재 사용자 추가
      workspace.getUsers().add(currentUser.getId());
      workspaceRepository.save(workspace);
    }

    Map<ObjectId, User> uMap = loadUsersForWorkspace(workspace);
    return toDto(workspace, uMap);
  }

  public WorkspaceResponseDto updateParticipantColors(String id,
      Map<String, String> participantColors) {
    Workspace workspace = workspaceRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));

    workspace.setParticipantColors(participantColors);
    workspaceRepository.save(workspace);

    Map<ObjectId, User> uMap = loadUsersForWorkspace(workspace);
    return toDto(workspace, uMap);
  }

  public WorkspaceScheduleResponseDto findWorkspaceSchedule(String id, String year, String month,
      String week, String day) {
    // helper methods for user joins
    return findWorkspaceScheduleInternal(id, year, month, week, day);
  }

  public WorkspaceScheduleResponseDto findWorkspaceScheduleFeed(String id,
      UserDetails userDetails) {
    // 현재 사용자 조회
    User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(
        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
    );

    ObjectId workspaceId;
    try {
      workspaceId = new ObjectId(id);
      log.debug("Finding workspace schedule feed for workspace ID: {}", workspaceId);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid workspace ID");
    }

    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));

    // 현재 날짜 기준으로 이번년도만 조회
    LocalDate today = LocalDate.now();
    int currentYear = today.getYear();
    ObjectId userId = user.getId();

    // 최적화: DB 레벨에서 날짜 + 참여자 필터링
    Date todayStart = Date.from(today.atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant());
    Date yearEnd = Date.from(LocalDate.of(currentYear, 12, 31)
        .plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant());

    List<Schedule> upcomingSchedules = scheduleRepository
        .findByWorkspaceAndStartDateAfterAndParticipant(workspaceId, todayStart, userId);

    // 이번년도 공휴일 조회
    List<HolidayDto> currentYearHolidays = holidayService.getHolidaysByYear(currentYear);

    // 오늘 이후 공휴일만 필터링
    List<HolidayDto> upcomingHolidays = currentYearHolidays.stream()
        .filter(h -> {
          LocalDate holidayDate = LocalDate.parse(h.getLocdate(), HOLIDAY_DATE_FORMATTER);
          return !holidayDate.isBefore(today);
        })
        .collect(Collectors.toList());

    // 유저 맵 구성 및 응답 빌드
    Map<ObjectId, User> userMap = buildUserMapForWorkspaceAndSchedules(workspace,
        upcomingSchedules);
    return buildResponse(workspace, upcomingSchedules, upcomingHolidays, userMap);
  }

  private Map<ObjectId, User> loadUsersForWorkspace(Workspace ws) {
    Set<ObjectId> ids = new HashSet<>();
    if (ws.getMaster() != null) {
      ids.add(ws.getMaster());
    }
    if (ws.getUsers() != null) {
      ids.addAll(ws.getUsers());
    }
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    List<User> users = userRepository.findAllById(ids);
    Map<ObjectId, User> map = new HashMap<>();
    for (User u : users) {
      if (u.getId() != null) {
        map.put(u.getId(), u);
      }
    }
    return map;
  }

  private Map<ObjectId, User> loadUsersForWorkspaces(List<Workspace> workspaces) {
    Set<ObjectId> ids = new HashSet<>();
    for (Workspace ws : workspaces) {
      if (ws.getMaster() != null) {
        ids.add(ws.getMaster());
      }
      if (ws.getUsers() != null) {
        ids.addAll(ws.getUsers());
      }
    }
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    List<User> users = userRepository.findAllById(ids);
    Map<ObjectId, User> map = new HashMap<>();
    for (User u : users) {
      if (u.getId() != null) {
        map.put(u.getId(), u);
      }
    }
    return map;
  }

  // 워크스페이스(master/users)와 스케줄 참여자까지 한 번에 로드
  private Map<ObjectId, User> buildUserMapForWorkspaceAndSchedules(Workspace ws,
      List<Schedule> schedules) {
    Set<ObjectId> ids = new HashSet<>();
    if (ws.getMaster() != null) {
      ids.add(ws.getMaster());
    }
    if (ws.getUsers() != null) {
      ids.addAll(ws.getUsers());
    }
    if (schedules != null) {
      for (Schedule sc : schedules) {
        if (sc.getParticipants() != null) {
          ids.addAll(sc.getParticipants());
        }
      }
    }
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    List<User> users = userRepository.findAllById(ids);
    Map<ObjectId, User> map = new HashMap<>();
    for (User u : users) {
      if (u.getId() != null) {
        map.put(u.getId(), u);
      }
    }
    return map;
  }

  private WorkspaceResponseDto toDto(Workspace ws, Map<ObjectId, User> userMap) {
    User master = ws.getMaster() != null ? userMap.get(ws.getMaster()) : null;
    UserResponseDto masterDto = master != null ? UserResponseDto.from(master) : null;

    List<UserResponseDto> userDtos = new ArrayList<>();
    if (ws.getUsers() != null) {
      for (ObjectId uid : ws.getUsers()) {
        User u = userMap.get(uid);
        if (u != null) {
          userDtos.add(UserResponseDto.from(u));
        }
      }
    }

    return WorkspaceResponseDto.builder()
        .id(ws.getId())
        .title(ws.getTitle())
        .master(masterDto)
        .users(userDtos)
        .participantColors(
            ws.getParticipantColors() != null ? ws.getParticipantColors() : new HashMap<>())
        .loveDay(ws.getLoveDay())
        .createdAt(ws.getCreatedAt())
        .updatedAt(ws.getUpdatedAt())
        .build();
  }

  // Split out actual logic to keep helpers above compile-safe
  private WorkspaceScheduleResponseDto findWorkspaceScheduleInternal(String id, String year,
      String month,
      String week, String day) {
    // 1. ObjectId 유효성 검사 및 워크스페이스 조회
    ObjectId workspaceId;
    try {
      workspaceId = new ObjectId(id);
      log.info("Finding workspace schedule for workspace ID: {}", workspaceId);

    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid workspace ID");
    }

    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));

    // 2. 스케줄 조회
    List<Schedule> schedules = scheduleRepository.findByWorkspace(workspaceId);

    // year가 없으면 모든 스케줄 반환
    if (Strings.isNullOrEmpty(year)) {
      Map<ObjectId, User> userMap = buildUserMapForWorkspaceAndSchedules(workspace, schedules);
      return buildResponse(workspace, schedules, ImmutableList.of(), userMap);
    }

    // 3. 공휴일 캘린더 조회
    List<HolidayDto> allHolidays = holidayService.getHolidaysByYear(Integer.parseInt(year));
    List<HolidayDto> filteredHolidays = filterHolidaysByPeriod(allHolidays, year, month, week, day);

    // 4. 스케줄 필터링
    List<Schedule> filteredSchedules = filterSchedulesByPeriod(schedules, year, month, week, day);

    // 5. 병합 및 정렬 (유저 조인 포함)
    Map<ObjectId, User> userMap = buildUserMapForWorkspaceAndSchedules(workspace,
        filteredSchedules);
    return buildResponse(workspace, filteredSchedules, filteredHolidays, userMap);
  }

  private List<HolidayDto> filterHolidaysByPeriod(List<HolidayDto> holidays, String year,
      String month, String week, String day) {
    if (holidays == null || holidays.isEmpty()) {
      return ImmutableList.of();
    }

    // year + month + day
    if (year != null && month != null && day != null) {
      String targetDate = String.format("%s%02d%02d", year, Integer.parseInt(month),
          Integer.parseInt(day));
      return holidays.stream()
          .filter(h -> h.getLocdate().equals(targetDate))
          .collect(Collectors.toList());
    }

    // year + month + week
    if (year != null && month != null && week != null) {
      LocalDate firstDayOfMonth = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
      LocalDate startOfWeek = firstDayOfMonth.plusWeeks(Integer.parseInt(week) - 1)
          .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
      LocalDate endOfWeek = startOfWeek.plusDays(6);

      return holidays.stream()
          .filter(h -> {
            LocalDate holidayDate = LocalDate.parse(h.getLocdate(), HOLIDAY_DATE_FORMATTER);
            return !holidayDate.isBefore(startOfWeek) && !holidayDate.isAfter(endOfWeek);
          })
          .collect(Collectors.toList());
    }

    // year + month
    if (year != null && month != null) {
      String yearMonth = String.format("%s%02d", year, Integer.parseInt(month));
      return holidays.stream()
          .filter(h -> h.getLocdate().startsWith(yearMonth))
          .collect(Collectors.toList());
    }

    // year only
    return holidays.stream()
        .filter(h -> h.getLocdate().startsWith(year))
        .collect(Collectors.toList());
  }

  private List<Schedule> filterSchedulesByPeriod(List<Schedule> schedules, String year,
      String month, String week, String day) {
    if (schedules == null || schedules.isEmpty()) {
      return ImmutableList.of();
    }

    // year + month + day
    if (year != null && month != null && day != null) {
      LocalDate targetDate = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month),
          Integer.parseInt(day));
      return schedules.stream()
          .filter(s -> {
            LocalDate scheduleDate = s.getStartDate().toInstant()
                .atZone(java.time.ZoneId.of("Asia/Seoul"))
                .toLocalDate();
            return scheduleDate.equals(targetDate);
          })
          .collect(Collectors.toList());
    }

    // year + month + week
    if (year != null && month != null && week != null) {
      LocalDate firstDayOfMonth = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
      LocalDate startOfWeek = firstDayOfMonth.plusWeeks(Integer.parseInt(week) - 1)
          .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
      LocalDate endOfWeek = startOfWeek.plusDays(6);

      return schedules.stream()
          .filter(s -> {
            LocalDate scheduleDate = s.getStartDate().toInstant()
                .atZone(java.time.ZoneId.of("Asia/Seoul"))
                .toLocalDate();
            return !scheduleDate.isBefore(startOfWeek) && !scheduleDate.isAfter(endOfWeek);
          })
          .collect(Collectors.toList());
    }

    // year + month
    if (year != null && month != null) {
      int targetYear = Integer.parseInt(year);
      int targetMonth = Integer.parseInt(month);
      return schedules.stream()
          .filter(s -> {
            LocalDate scheduleDate = s.getStartDate().toInstant()
                .atZone(java.time.ZoneId.of("Asia/Seoul"))
                .toLocalDate();
            return scheduleDate.getYear() == targetYear
                && scheduleDate.getMonthValue() == targetMonth;
          })
          .collect(Collectors.toList());
    }

    // year only
    return schedules.stream()
        .filter(s -> {
          assert year != null;
          LocalDate scheduleDate = s.getStartDate().toInstant()
              .atZone(java.time.ZoneId.of("Asia/Seoul"))
              .toLocalDate();
          return scheduleDate.getYear() == Integer.parseInt(year);
        })
        .collect(Collectors.toList());
  }

  private WorkspaceScheduleResponseDto buildResponse(Workspace workspace,
      List<Schedule> schedules, List<HolidayDto> holidays, Map<ObjectId, User> userMap) {
    List<WorkspaceScheduleItemDto> combinedSchedule = Lists.newArrayList();

    // 공휴일 추가
    holidays.forEach(holiday -> {
      LocalDate date = LocalDate.parse(holiday.getLocdate(), HOLIDAY_DATE_FORMATTER);
      Date holidayDate = Date.from(date.atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant());

      combinedSchedule.add(WorkspaceScheduleItemDto.builder()
          .startDate(holidayDate)
          .endDate(holidayDate)
          .title(holiday.getDateName())
          .description("Y".equals(holiday.getIsHoliday()) ? "Public Holiday" : "Workday")
          .participants(ImmutableList.<UserResponseDto>of())
          .isHoliday(true)
          .build());
    });

    // 스케줄 추가
    schedules.forEach(schedule -> {
      List<UserResponseDto> participantDtos = schedule.getParticipants() != null
          ? schedule.getParticipants().stream()
          .map(userMap::get)
          .filter(java.util.Objects::nonNull)
          .map(UserResponseDto::from)
          .collect(Collectors.toList())
          : ImmutableList.of();

      combinedSchedule.add(WorkspaceScheduleItemDto.builder()
          .id(schedule.getId())
          .startDate(schedule.getStartDate())
          .endDate(schedule.getEndDate())
          .title(schedule.getTitle())
          .memo(schedule.getMemo())
          .participants(participantDtos)
          .isHoliday(false)
          .isAllDay(schedule.getIsAllDay())
          .repeatType(schedule.getRepeatType())
          .calendarType(schedule.getCalendarType())
          .reminderMinutes(schedule.getReminderMinutes())
          .isImportant(schedule.getIsImportant())
          .build());
    });

    // 날짜별 정렬
    combinedSchedule.sort(Comparator.comparing(WorkspaceScheduleItemDto::getStartDate,
        Comparator.nullsLast(Comparator.naturalOrder())));

    User master = workspace.getMaster() != null ? userMap.get(workspace.getMaster()) : null;
    List<UserResponseDto> userDtos = workspace.getUsers() != null
        ? workspace.getUsers().stream()
        .map(userMap::get)
        .filter(java.util.Objects::nonNull)
        .map(UserResponseDto::from)
        .collect(Collectors.toList())
        : new ArrayList<>();

    return WorkspaceScheduleResponseDto.builder()
        .id(workspace.getId())
        .master(master != null ? UserResponseDto.from(master) : null)
        .users(userDtos)
        .loveDay(workspace.getLoveDay())
        .schedules(combinedSchedule)
        .build();
  }

}
