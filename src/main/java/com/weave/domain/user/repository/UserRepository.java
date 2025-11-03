package com.weave.domain.user.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.weave.domain.user.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByInviteCode(String inviteCode);
    Optional<User> findByEmail(String email);
    Optional<User> findByInviteCode(String inviteCode);
}
