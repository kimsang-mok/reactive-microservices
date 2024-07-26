package com.kimsang.microservices.core.review.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews", indexes = {@Index(name = "reviews_unique_idx", unique = true, columnList = "productId," +
    "reviewId")})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {

  @Id @GeneratedValue
  private int id;

  @Version
  private int version;

  private int productId;
  private int reviewId;
  private String author;
  private String subject;
  private String content;
}
