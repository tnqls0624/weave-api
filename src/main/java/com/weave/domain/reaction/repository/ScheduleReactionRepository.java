package com.weave.domain.reaction.repository;

import com.weave.domain.reaction.entity.ScheduleReaction;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleReactionRepository extends MongoRepository<ScheduleReaction, ObjectId> {

  List<ScheduleReaction> findByScheduleId(ObjectId scheduleId);

  Optional<ScheduleReaction> findByScheduleIdAndUserIdAndEmoji(ObjectId scheduleId, ObjectId userId, String emoji);

  void deleteByScheduleIdAndUserIdAndEmoji(ObjectId scheduleId, ObjectId userId, String emoji);

  long countByScheduleIdAndEmoji(ObjectId scheduleId, String emoji);
}
