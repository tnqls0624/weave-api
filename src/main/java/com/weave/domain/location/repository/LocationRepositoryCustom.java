package com.weave.domain.location.repository;

import com.weave.domain.location.entity.Location;
import org.bson.types.ObjectId;

public interface LocationRepositoryCustom {

  /**
   * 사용자의 위치 정보를 upsert (있으면 업데이트, 없으면 삽입)
   * workspace_id + user_id 기준으로 단일 문서 유지
   */
  Location upsertLocation(ObjectId workspaceId, ObjectId userId, Double latitude, Double longitude);
}
