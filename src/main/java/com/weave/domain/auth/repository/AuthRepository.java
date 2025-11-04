package com.weave.domain.auth.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.weave.domain.user.entity.User;

import java.util.Optional;

@Repository
public interface AuthRepository extends MongoRepository<User, ObjectId> {
    Optional<User> findByEmail(String email);
}

