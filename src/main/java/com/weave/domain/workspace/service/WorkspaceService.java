package com.weave.domain.workspace.service;

import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.domain.workspace.dto.CreateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.UpdateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.WorkspaceResponseDto;
import com.weave.domain.workspace.entity.Workspace;
import com.weave.domain.workspace.repository.WorkspaceRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;

  public WorkspaceResponseDto create(CreateWorkspaceRequestDto dto, String email) {
    // 초대코드 확인(마스터)
    User master = userRepository.findByInviteCode(dto.getInviteCode()).orElseThrow(
        () -> new BusinessException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 초대코드 입니다.")
    );

    // 참가자 확인(게스트)
    User user = userRepository.findByEmail(email).orElseThrow(
        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
    );

    // 자기 자신이 초대했는지 확인
    if (master.getId().equals(user.getId())) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "자기 자신은 초대할 수 없습니다.");
    }

    List<ObjectId> users = List.of(master.getId(), user.getId());

    // 워크스페이스 생성 및 저장
    Workspace workspace = workspaceRepository.save(
        Workspace.builder()
            .master(master.getId())
            .users(users)
            .loveDay(dto.getLoveDay())
            .thumbnailImage(dto.getThumbnailImage())
            .build()
    );
    return WorkspaceResponseDto.from(workspace);
  }

  public WorkspaceResponseDto update(UpdateWorkspaceRequestDto dto, String id) {
    Workspace workspace = workspaceRepository.findById(new ObjectId(id)).orElseThrow(
        () -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)
    );

    workspace.setLoveDay(dto.getLoveDay());
    workspace.setThumbnailImage(dto.getThumbnailImage());
    workspaceRepository.save(workspace);

    return WorkspaceResponseDto.from(workspace);
  }

  public WorkspaceResponseDto findById(String id) {
    Workspace workspace = workspaceRepository.findById(new ObjectId(id)).orElseThrow(
        () -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)
    );
    return WorkspaceResponseDto.from(workspace);
  }

  public WorkspaceResponseDto[] find(String email) {
    User user = userRepository.findByEmail(email).orElseThrow(
        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
    );

    List<Workspace> workspaces = workspaceRepository.findByUsersContaining(user.getId());
    return workspaces.stream().map(WorkspaceResponseDto::from).toArray(WorkspaceResponseDto[]::new);
  }

}
