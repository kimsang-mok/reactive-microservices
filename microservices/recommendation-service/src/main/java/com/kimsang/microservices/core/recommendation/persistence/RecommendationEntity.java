package com.kimsang.microservices.core.recommendation.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection =  "recommendations")
@CompoundIndex(name= "prod-rec-id", unique =true, def = "{'productId': 1, 'recommendationId': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationEntity {

  @Id
  private String id;

  @Version
  private Integer version;

  private int productId;
  private int recommendationId;
  private String author;
  private int rating;
  private String content;
}
