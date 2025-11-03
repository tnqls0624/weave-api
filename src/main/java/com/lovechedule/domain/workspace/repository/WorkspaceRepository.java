package com.lovechedule.domain.workspace.repository;

import com.lovechedule.domain.workspace.entity.Workspace;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceRepository extends MongoRepository<Workspace, String> {
    Optional<Workspace> findById(String id);
}
