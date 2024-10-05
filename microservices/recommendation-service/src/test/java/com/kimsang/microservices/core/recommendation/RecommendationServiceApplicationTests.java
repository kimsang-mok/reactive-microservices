package com.kimsang.microservices.core.recommendation;

import com.kimsang.api.core.recommendation.Recommendation;
import com.kimsang.microservices.core.recommendation.persistence.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class RecommendationServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @LocalServerPort
  private int port;

  @Autowired
  private RecommendationRepository repository;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();

    // to fix timeout error
    client = WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .responseTimeout(Duration.ofSeconds(10))
        .build();
  }

  @Test
  void getRecommendationsByProductId() {
    int productId = 1;

    postAndVerifyRecommendation(productId, 1, OK);
    postAndVerifyRecommendation(productId, 2, OK);
    postAndVerifyRecommendation(productId, 3, OK);

    assertEquals(3, repository.findByProductId(productId).size());

    getAndVerifyRecommendationsByProductId(productId, OK)
        .jsonPath("$.length()").isEqualTo(3)
        .jsonPath("$[1].productId").isEqualTo(productId)
        .jsonPath("$[2].recommendationId").isEqualTo(3);

  }

  @Test
  void duplicateError() {
    int productId = 1;
    int recommendationId = 1;

    postAndVerifyRecommendation(productId, recommendationId, OK)
        .jsonPath("$.productId").isEqualTo(productId)
        .jsonPath("$.recommendationId").isEqualTo(recommendationId);

    assertEquals(1, repository.count());

    postAndVerifyRecommendation(productId, recommendationId, UNPROCESSABLE_ENTITY)
        .jsonPath("$.path").isEqualTo("/recommendation")
        .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Recommendation Id: 1");

    assertEquals(1, repository.count());
  }

  @Test
  void deleteRecommendations() {

    int productId = 1;
    int recommendationId = 1;

    postAndVerifyRecommendation(productId, recommendationId, OK);
    assertEquals(1, repository.findByProductId(productId).size());

    deleteAndVerifyRecommendationsByProductId(productId, OK);
    assertEquals(0, repository.findByProductId(productId).size());

    deleteAndVerifyRecommendationsByProductId(productId, OK);
  }

  @Test
  void getRecommendationsMissingParameter() {

    getAndVerifyRecommendationsByProductId("", BAD_REQUEST)
        .jsonPath("$.path").isEqualTo("/recommendation")
        .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getRecommendationsInvalidParameter() {

    getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
        .jsonPath("$.path").isEqualTo("/recommendation")
        .jsonPath("$.message").isEqualTo("Type mismatch.");
  }


  @Test
  void getRecommendationsNotFound() {

    getAndVerifyRecommendationsByProductId("?productId=113", OK)
        .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getRecommendationsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
        .jsonPath("$.path").isEqualTo("/recommendation")
        .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus httpStatus) {
    return getAndVerifyRecommendationsByProductId("?productId=" + productId, httpStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus httpStatus) {
    return client.get()
        .uri("/recommendation" + productIdQuery)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody();

  }

  private WebTestClient.BodyContentSpec postAndVerifyRecommendation(
      int productId,
      int recommendationId,
      HttpStatus expectedHttpStatus
  ) {
    Recommendation recommendation = createSimpleRecommendation(productId, recommendationId);

    return client.post()
        .uri("/recommendation")
        .body(just(recommendation), Recommendation.class)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(expectedHttpStatus)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody();
  }

  private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(
      int productId, HttpStatus expectedStatus) {
    return client.delete()
        .uri("/recommendation?productId=" + productId)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus)
        .expectBody();
  }

  private Recommendation createSimpleRecommendation(int productId, int recommendationId) {
    return new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId,
        "Content " + recommendationId, "SA");
  }
}
