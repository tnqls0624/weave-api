package com.weave.domain.reaction.entity;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "schedule_reactions")
@CompoundIndex(name = "schedule_user_emoji_idx", def = "{'schedule_id': 1, 'user_id': 1, 'emoji': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleReaction {

  @Id
  private ObjectId id;

  @Field("schedule_id")
  @Indexed
  private ObjectId scheduleId;

  @Field("user_id")
  @Indexed
  private ObjectId userId;

  @Field("emoji")
  private String emoji;  // 👍, ❤️, 🎉, 👀, 🙏, 😢

  @CreatedDate
  @Field("created_at")
  private Date createdAt;
}
