package com.weave.domain.workspace.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workspaces")
public class Workspace {

  @Id
  private ObjectId id;

  @Field("title")
  private String title;

  @Field("master")
  @Indexed
  private ObjectId master;

  @Field("users")
  @Indexed
  private List<ObjectId> users;

  @Field("participant_colors")
  private Map<String, String> participantColors; // key: userId, value: color code (e.g., "#FF5733")

  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  private Date updatedAt;
}
