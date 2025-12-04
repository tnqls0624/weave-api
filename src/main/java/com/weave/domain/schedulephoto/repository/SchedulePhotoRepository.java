package com.weave.domain.schedulephoto.repository;

import com.weave.domain.schedulephoto.entity.SchedulePhoto;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulePhotoRepository extends MongoRepository<SchedulePhoto, ObjectId> {

  List<SchedulePhoto> findByScheduleIdOrderByUploadedAtDesc(ObjectId scheduleId);

  void deleteByScheduleId(ObjectId scheduleId);
}
