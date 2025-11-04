package com.weave.domain.workspace.repository;

import com.weave.domain.workspace.entity.Workspace;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends MongoRepository<Workspace, ObjectId> {

  List<Workspace> findByUsersContaining(ObjectId userId);
}
