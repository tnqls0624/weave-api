package com.weave.domain.workspace.dto;

import com.weave.domain.user.dto.UserResponseDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceScheduleResponseDto {

  private ObjectId id;
  private UserResponseDto master;
  private List<UserResponseDto> users;
  private String loveDay;
  private List<WorkspaceScheduleItemDto> schedules;
}
