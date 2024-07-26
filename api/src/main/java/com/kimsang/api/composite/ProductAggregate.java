package com.kimsang.api.composite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class ProductAggregate {
  private final int productId;
  private final String name;
  private final int weight;
  private final ServiceAddresses serviceAddresses;

  public ProductAggregate() {
    productId = 0;
    name = null;
    weight = 0;
    serviceAddresses = null;
  }
}
