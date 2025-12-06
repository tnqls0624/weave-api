package com.weave.domain.comment.repository;

import com.weave.domain.comment.entity.Comment;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends MongoRepository<Comment, ObjectId> {

  List<Comment> findByScheduleIdOrderByCreatedAtAsc(ObjectId scheduleId);

  // 부모 댓글만 조회 (일반 댓글)
  List<Comment> findByScheduleIdAndParentIdIsNullOrderByCreatedAtAsc(ObjectId scheduleId);

  // 특정 댓글의 답글 조회
  List<Comment> findByParentIdOrderByCreatedAtAsc(ObjectId parentId);

  // 일정의 댓글 수
  long countByScheduleId(ObjectId scheduleId);

  void deleteByScheduleId(ObjectId scheduleId);
}
