package com.weave.domain.auth.repository;

import com.weave.domain.user.entity.User;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends MongoRepository<User, ObjectId> {

  Optional<User> findByEmail(String email);
}

