package com.weave.domain.location.repository;

import com.weave.domain.location.entity.Location;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends MongoRepository<Location, ObjectId> {

  List<Location> findByWorkspaceIdOrderByTimestampDesc(ObjectId workspaceId);

  List<Location> findByWorkspaceIdAndUserIdOrderByTimestampDesc(ObjectId workspaceId,
      ObjectId userId);

  /**
   * 워크스페이스의 각 사용자별 최신 위치만 조회 (Aggregation 사용)
   * - $match: 워크스페이스 필터링
   * - $sort: timestamp 내림차순
   * - $group: 사용자별 첫 번째(최신) 문서만 선택
   */
  @Aggregation(pipeline = {
      "{ $match: { workspace_id: ?0 } }",
      "{ $sort: { timestamp: -1 } }",
      "{ $group: { _id: '$user_id', doc: { $first: '$$ROOT' } } }",
      "{ $replaceRoot: { newRoot: '$doc' } }"
  })
  List<Location> findLatestLocationsByWorkspaceId(ObjectId workspaceId);

  /**
   * 특정 사용자의 최신 위치 1개만 조회
   */
  Optional<Location> findFirstByWorkspaceIdAndUserIdOrderByTimestampDesc(
      ObjectId workspaceId, ObjectId userId);
}
