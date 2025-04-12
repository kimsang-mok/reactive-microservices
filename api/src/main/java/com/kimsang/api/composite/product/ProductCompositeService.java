package com.kimsang.api.composite.product;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SecurityRequirement(name = "security_auth")
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
  Mono<ProductAggregate> getProduct(
      @PathVariable int productId,
      @RequestParam(value = "delay", required = false,
          defaultValue = "0") int delay,
      @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent);

  @ResponseStatus(HttpStatus.ACCEPTED)
  @DeleteMapping(value = "/product-composite/{productId}")
  Mono<Void> deleteProduct(@PathVariable int productId);

  @GetMapping(
      value = "/product-composite/hello",
      produces = "application/json"
  )
  String sayHello();
}
