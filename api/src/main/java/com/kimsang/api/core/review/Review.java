package com.kimsang.api.core.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Review {
  private int productId;
  private int reviewId;
  private String author;
  private String subject;
  private String content;
  private String serviceAddress;

  public Review(int productId, int reviewId, String author, String subject, String content, String serviceAddress) {
   this.productId = productId;
   this.reviewId = reviewId;
   this.author = author;
   this.subject = subject;
   this.content = content;
   this.serviceAddress = serviceAddress;
  }
}
