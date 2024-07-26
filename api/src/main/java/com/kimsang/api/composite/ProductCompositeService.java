package com.kimsang.api.composite;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ProductCompositeService {
  @PostMapping(
      value = "/product-composite",
      consumes =  "application/json"
  )
  void createProduct(@RequestBody ProductAggregate body);

  @GetMapping(
      value = "/product-composite/{productId}",
      produces = "application/json"
  )
  ProductAggregate getProduct(@PathVariable int productId);

  @GetMapping(
      value = "/hello",
      produces = "application/json"
  )
  String sayHello();
}
