package com.weave.domain.checklist.service;

import com.weave.domain.checklist.dto.ChecklistItemDto;
import com.weave.domain.checklist.dto.CreateChecklistItemDto;
import com.weave.domain.checklist.entity.ChecklistItem;
import com.weave.domain.checklist.repository.ChecklistItemRepository;
import com.weave.global.exception.BusinessException;
import com.weave.global.exception.ErrorCode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChecklistService {

  private final ChecklistItemRepository checklistItemRepository;

  private static final SimpleDateFormat dateFormat;

  static {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public List<ChecklistItemDto> getChecklist(String scheduleId) {
    ObjectId scheduleOid = new ObjectId(scheduleId);
    List<ChecklistItem> items = checklistItemRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleOid);
    return items.stream().map(this::toDto).collect(Collectors.toList());
  }

  @Transactional
  public ChecklistItemDto addItem(String scheduleId, CreateChecklistItemDto dto, String userId) {
    ObjectId scheduleOid = new ObjectId(scheduleId);
    ObjectId userOid = new ObjectId(userId);

    ChecklistItem item = ChecklistItem.builder()
        .scheduleId(scheduleOid)
        .content(dto.getContent())
        .isCompleted(false)
        .createdBy(userOid)
        .createdAt(new Date())
        .build();

    ChecklistItem saved = checklistItemRepository.save(item);
    return toDto(saved);
  }

  @Transactional
  public ChecklistItemDto toggleItem(String scheduleId, String itemId, String userId) {
    ObjectId itemOid = new ObjectId(itemId);
    ObjectId userOid = new ObjectId(userId);

    ChecklistItem item = checklistItemRepository.findById(itemOid)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (item.getIsCompleted()) {
      item.setIsCompleted(false);
      item.setCompletedBy(null);
      item.setCompletedAt(null);
    } else {
      item.setIsCompleted(true);
      item.setCompletedBy(userOid);
      item.setCompletedAt(new Date());
    }

    ChecklistItem saved = checklistItemRepository.save(item);
    return toDto(saved);
  }

  @Transactional
  public void deleteItem(String scheduleId, String itemId) {
    ObjectId itemOid = new ObjectId(itemId);
    checklistItemRepository.deleteById(itemOid);
  }

  private ChecklistItemDto toDto(ChecklistItem item) {
    return ChecklistItemDto.builder()
        .id(item.getId().toHexString())
        .content(item.getContent())
        .isCompleted(item.getIsCompleted())
        .completedBy(item.getCompletedBy() != null ? item.getCompletedBy().toHexString() : null)
        .completedAt(item.getCompletedAt() != null ? dateFormat.format(item.getCompletedAt()) : null)
        .createdBy(item.getCreatedBy() != null ? item.getCreatedBy().toHexString() : null)
        .createdAt(item.getCreatedAt() != null ? dateFormat.format(item.getCreatedAt()) : null)
        .build();
  }
}
