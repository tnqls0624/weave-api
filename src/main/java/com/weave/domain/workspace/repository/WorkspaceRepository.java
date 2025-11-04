package com.weave.domain.workspace.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.weave.domain.workspace.entity.Workspace;

import java.util.List;

@Repository
public interface WorkspaceRepository extends MongoRepository<Workspace, ObjectId> {
    List<Workspace> findByUsersContaining(ObjectId userId);
}
