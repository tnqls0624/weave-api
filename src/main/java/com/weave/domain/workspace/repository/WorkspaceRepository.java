package com.weave.domain.workspace.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.weave.domain.workspace.entity.Workspace;

import java.util.Optional;

@Repository
public interface WorkspaceRepository extends MongoRepository<Workspace, String> {
    Optional<Workspace> findById(String id);
}
