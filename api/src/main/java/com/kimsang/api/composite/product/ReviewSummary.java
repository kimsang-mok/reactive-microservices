package com.kimsang.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewSummary {

  private final int reviewId;
  private final String author;
  private final String subject;
  private final String content;

  public ReviewSummary() {
    this.reviewId = 0;
    this.author = null;
    this.subject = null;
    this.content = null;
  }
}