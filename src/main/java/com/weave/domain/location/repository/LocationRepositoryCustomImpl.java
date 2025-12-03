package com.weave.domain.location.repository;

import com.weave.domain.location.entity.Location;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LocationRepositoryCustomImpl implements LocationRepositoryCustom {

  private final MongoTemplate mongoTemplate;

  @Override
  public Location upsertLocation(ObjectId workspaceId, ObjectId userId, Double latitude, Double longitude) {
    Query query = new Query(Criteria.where("workspace_id").is(workspaceId)
        .and("user_id").is(userId));

    Update update = new Update()
        .set("latitude", latitude)
        .set("longitude", longitude)
        .set("timestamp", new Date())
        .setOnInsert("workspace_id", workspaceId)
        .setOnInsert("user_id", userId);

    FindAndModifyOptions options = FindAndModifyOptions.options()
        .returnNew(true)
        .upsert(true);

    return mongoTemplate.findAndModify(query, update, options, Location.class);
  }
}
