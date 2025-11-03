package com.lovechedule.domain.user.repository;

import com.lovechedule.domain.user.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByInviteCode(String inviteCode);
    Optional<User> findByEmail(String email);
    Optional<User> findByInviteCode(String inviteCode);
}
