package com.weave.domain.location.repository;

import com.weave.domain.location.entity.Location;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends MongoRepository<Location, ObjectId> {

  List<Location> findByWorkspaceIdOrderByTimestampDesc(ObjectId workspaceId);

  List<Location> findByWorkspaceIdAndUserIdOrderByTimestampDesc(ObjectId workspaceId,
      ObjectId userId);
}
