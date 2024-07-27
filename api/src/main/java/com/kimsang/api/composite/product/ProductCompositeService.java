package com.kimsang.api.composite.product;

import org.springframework.web.bind.annotation.*;

public interface ProductCompositeService {

  @PostMapping(
      value = "/product-composite",
      consumes = "application/json"
  )
  void createProduct(@RequestBody ProductAggregate body);

  @GetMapping(
      value = "/product-composite/{productId}",
      produces = "application/json"
  )
  ProductAggregate getProduct(@PathVariable int productId);

  @DeleteMapping(value = "/product-composite/{productId}")
  void deleteProduct(@PathVariable int productId);

  @GetMapping(
      value = "/hello",
      produces = "application/json"
  )
  String sayHello();
}
