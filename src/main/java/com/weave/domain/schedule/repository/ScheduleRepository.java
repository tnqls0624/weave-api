package com.weave.domain.schedule.repository;

import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.user.entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScheduleRepository extends MongoRepository<Schedule, String> {
}

