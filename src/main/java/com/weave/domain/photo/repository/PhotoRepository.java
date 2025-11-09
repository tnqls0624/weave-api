package com.weave.domain.photo.repository;

import com.weave.domain.photo.entity.Photo;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PhotoRepository extends MongoRepository<Photo, ObjectId> {
  Optional<Photo> findByHash(String hash);
}
