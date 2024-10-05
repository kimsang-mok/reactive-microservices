package com.kimsang.microservices.core.product;

import com.kimsang.api.core.product.Product;
import com.kimsang.microservices.core.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;


/**
 * WebTestClient is a Spring utility for testing reactive APIs. This is being used to simulate and verify requests to
 * the service without actually spinning up a web server.
 */

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductServiceApplicationTests extends MongoDbTestBase {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProductRepository repository;

  @LocalServerPort
  private int port;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();

    // to fix timeout error
    client = WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .responseTimeout(Duration.ofSeconds(10))
        .build();
  }

  @Test
  void getProductById() {
    int productId = 1;

    assertNull(repository.findByProductId(productId).block());
    assertEquals(0, repository.count().block());

//    postAndVerifyProduct(productId, OK);


    getAndVerifyProduct(productId, OK).jsonPath("$.productId").isEqualTo(productId);
  }

  @Test
  void duplicateError() {
    int productId = 1;

    postAndVerifyProduct(productId, OK);

    assertTrue(repository.findByProductId(productId).isPresent());

    postAndVerifyProduct(productId, UNPROCESSABLE_ENTITY)
        .jsonPath("$.path").isEqualTo("/product")
        .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
  }

  @Test
  void deleteProduct() {
    int productId = 1;

    postAndVerifyProduct(productId, OK);
    assertTrue(repository.findByProductId(productId).isPresent());

    deleteAndVerifyProduct(productId, OK);
    assertFalse(repository.findByProductId(productId).isPresent());

    deleteAndVerifyProduct(productId, OK);
  }

  @Test
  void getProductInvalidParameterString() {
    getAndVerifyProduct("/no-integer", BAD_REQUEST)
        .jsonPath("$.path").isEqualTo("/product/no-integer")
        .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getProductNotFound() {
    int productIdNotFound = 13;
    getAndVerifyProduct(productIdNotFound, NOT_FOUND)
        .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
        .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
  }

  @Test
  void getProductInvalidParameterNegativeValue() {
    int productIdInvalid = -1;

    getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_ENTITY)
        .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
        .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);

  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return getAndVerifyProduct("/" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
    return client.get()
        .uri("/product" + productIdPath)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody();
  }

  private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    Product product = createSimpleProduct(productId);
    return client.post()
        .uri("/product")
        .body(just(product), Product.class)
        .accept(APPLICATION_JSON)
        .exchange() // sends the request and waits for the response
        .expectStatus().isEqualTo(expectedStatus)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody();
  }

  private WebTestClient.BodyContentSpec deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return client.delete()
        .uri("/product/" + productId)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus)
        .expectBody();
  }

  private Product createSimpleProduct(int productId) {
    return new Product(productId, "Name " + productId, productId, "SA");
  }
}
