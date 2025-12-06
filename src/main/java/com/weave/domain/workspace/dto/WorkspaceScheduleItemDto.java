package com.weave.domain.workspace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weave.domain.user.dto.UserResponseDto;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkspaceScheduleItemDto {

  private ObjectId id;
  private Date startDate;  // 스케줄용
  private Date endDate;  // 스케줄용
  private String title;
  private String description;  // 공휴일용
  private String memo;  // 스케줄용
  private List<UserResponseDto> participants; // 참여자 정보로 응답
  private Map<String, String> participantColors;  // 참여자별 색상 (userId: colorCode)
  private Boolean isHoliday;
  private Boolean isAllDay;  // 종일 일정 여부
  private String repeatType;
  private String calendarType;
  private Integer reminderMinutes;  // 알림 시간 (분 단위)
  private Boolean isImportant;  // 중요 일정 여부
  private Long commentCount;  // 댓글 수
}
