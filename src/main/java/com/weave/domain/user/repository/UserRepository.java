package com.weave.domain.user.repository;

import com.weave.domain.user.entity.User;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

  boolean existsByInviteCode(String inviteCode);

  Optional<User> findByEmail(String email);

  Optional<User> findByInviteCode(String inviteCode);

  // 삭제되지 않은 사용자만 조회
  Optional<User> findByEmailAndDeletedFalse(String email);

  Optional<User> findByInviteCodeAndDeletedFalse(String inviteCode);

  boolean existsByInviteCodeAndDeletedFalse(String inviteCode);
}
