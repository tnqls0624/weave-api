package com.weave.domain.reaction.repository;

import com.weave.domain.reaction.entity.CommentReaction;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReactionRepository extends MongoRepository<CommentReaction, ObjectId> {

  List<CommentReaction> findByCommentId(ObjectId commentId);

  Optional<CommentReaction> findByCommentIdAndUserIdAndEmoji(ObjectId commentId, ObjectId userId, String emoji);

  void deleteByCommentIdAndUserIdAndEmoji(ObjectId commentId, ObjectId userId, String emoji);

  long countByCommentIdAndEmoji(ObjectId commentId, String emoji);
}
