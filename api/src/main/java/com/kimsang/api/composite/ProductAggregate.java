package com.kimsang.api.composite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductAggregate {
  private final int productId;
  private final String name;
  private final int weight;
  private final List<RecommendationSummary> recommendations;
  private final List<ReviewSummary> reviews;
  private final ServiceAddresses serviceAddresses;

  public ProductAggregate() {
    productId = 0;
    name = null;
    weight = 0;
    recommendations = null;
    reviews = null;
    serviceAddresses = null;
  }
}
