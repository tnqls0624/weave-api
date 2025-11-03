package com.lovechedule.domain.workspace.service;

import com.lovechedule.domain.user.entity.User;
import com.lovechedule.domain.user.repository.UserRepository;
import com.lovechedule.domain.workspace.dto.CreateWorkspaceRequestDto;
import com.lovechedule.domain.workspace.dto.UpdateWorkspaceRequestDto;
import com.lovechedule.domain.workspace.dto.WorkspaceResponseDto;
import com.lovechedule.domain.workspace.entity.Workspace;
import com.lovechedule.domain.workspace.repository.WorkspaceRepository;
import com.lovechedule.global.BusinessException;
import com.lovechedule.global.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;


    public WorkspaceResponseDto createWorkspace(CreateWorkspaceRequestDto dto, String email) {
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
                        .tags(dto.getTags())
                        .build()
        );

        // DTO로 변환
        return WorkspaceResponseDto.builder()
                .id(workspace.getId())
                .master(workspace.getMaster())
                .users(workspace.getUsers())
                .tags(workspace.getTags())
                .loveDay(workspace.getLoveDay())
                .thumbnailImage(workspace.getThumbnailImage())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    public WorkspaceResponseDto updateWorkspace(UpdateWorkspaceRequestDto dto, String id) {
        Workspace workspace = workspaceRepository.findById(id).orElseThrow(
                () -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)
        );

        workspace.setLoveDay(dto.getLoveDay());
        workspace.setTags(dto.getTags());
        workspace.setThumbnailImage(dto.getThumbnailImage());
        workspaceRepository.save(workspace);

        return WorkspaceResponseDto.builder()
                .id(workspace.getId())
                .master(workspace.getMaster())
                .users(workspace.getUsers())
                .tags(workspace.getTags())
                .loveDay(workspace.getLoveDay())
                .thumbnailImage(workspace.getThumbnailImage())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

}
