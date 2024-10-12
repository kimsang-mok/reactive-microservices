package com.kimsang.api.composite.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface ProductCompositeService {

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping(
      value = "/product-composite",
      consumes = "application/json"
  )
  Mono<Void> createProduct(@RequestBody ProductAggregate body);

  @GetMapping(
      value = "/product-composite/{productId}",
      produces = "application/json"
  )
  Mono<ProductAggregate> getProduct(@PathVariable int productId);

  @ResponseStatus(HttpStatus.ACCEPTED)
  @DeleteMapping(value = "/product-composite/{productId}")
  Mono<Void> deleteProduct(@PathVariable int productId);

  @GetMapping(
      value = "/hello",
      produces = "application/json"
  )
  String sayHello();
}
