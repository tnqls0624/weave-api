package com.weave.domain.comment.repository;

import com.weave.domain.comment.entity.Comment;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends MongoRepository<Comment, ObjectId> {

  List<Comment> findByScheduleIdOrderByCreatedAtAsc(ObjectId scheduleId);

  void deleteByScheduleId(ObjectId scheduleId);
}
