package com.weave.domain.locationreminder.repository;

import com.weave.domain.locationreminder.entity.LocationReminder;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationReminderRepository extends MongoRepository<LocationReminder, ObjectId> {

  Optional<LocationReminder> findByScheduleId(ObjectId scheduleId);

  void deleteByScheduleId(ObjectId scheduleId);
}
