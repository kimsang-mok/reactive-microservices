package com.kimsang.microservices.core.product.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "products")
public class ProductEntity {
  @Id
  private String id;

  @Version
  private Integer version;

  @Indexed(unique = true)
  private int productId;

  private String name;
  private int weight;

  public ProductEntity(int productId, String name, int weight) {
    this.productId = productId;
    this.name = name;
    this.weight = weight;
  }
}

