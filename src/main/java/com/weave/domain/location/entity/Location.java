package com.weave.domain.location.entity;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "locations")
@CompoundIndex(name = "workspace_user_idx", def = "{'workspace_id': 1, 'user_id': 1}")
@CompoundIndex(name = "workspace_timestamp_idx", def = "{'workspace_id': 1, 'timestamp': -1}")
@CompoundIndex(name = "workspace_user_timestamp_idx", def = "{'workspace_id': 1, 'user_id': 1, 'timestamp': -1}")
public class Location {

  @Id
  private ObjectId id;

  @Field("workspace_id")
  private ObjectId workspaceId;

  @Field("user_id")
  private ObjectId userId;

  @Field("latitude")
  private Double latitude;

  @Field("longitude")
  private Double longitude;

  @CreatedDate
  private Date timestamp;
}
