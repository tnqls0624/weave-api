package com.weave.domain.auth.repository;

import com.weave.domain.user.entity.User;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends MongoRepository<User, ObjectId> {

  Optional<User> findByEmail(String email);

  // 삭제되지 않은 사용자만 조회
  Optional<User> findByEmailAndDeletedFalse(String email);
}

