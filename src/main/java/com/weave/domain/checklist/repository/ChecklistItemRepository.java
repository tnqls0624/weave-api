package com.weave.domain.checklist.repository;

import com.weave.domain.checklist.entity.ChecklistItem;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistItemRepository extends MongoRepository<ChecklistItem, ObjectId> {

  List<ChecklistItem> findByScheduleIdOrderByCreatedAtAsc(ObjectId scheduleId);

  void deleteByScheduleId(ObjectId scheduleId);
}
