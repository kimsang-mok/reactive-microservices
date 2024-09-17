package com.kimsang.microservices.composite.product.services;


import static java.util.logging.Level.FINE;
import static reactor.core.publisher.Flux.empty;
import static com.kimsang.api.event.Event.Type.CREATE;
import static com.kimsang.api.event.Event.Type.DELETE;


import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kimsang.api.core.product.Product;
import com.kimsang.api.core.product.ProductService;
import com.kimsang.api.core.recommendation.Recommendation;
import com.kimsang.api.core.recommendation.RecommendationService;
import com.kimsang.api.core.review.Review;
import com.kimsang.api.core.review.ReviewService;
import com.kimsang.api.event.Event;
import com.kimsang.api.exceptions.InvalidInputException;
import com.kimsang.api.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.kimsang.util.http.HttpErrorInfo;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private final WebClient webClient;
  private final ObjectMapper mapper;

  private final String productServiceUrl;
  private final String recommendationServiceUrl;
  private final String reviewServiceUrl;

  private final StreamBridge streamBridge;
  private final Scheduler publishEventScheduler;

  @Autowired
  public ProductCompositeIntegration(
      WebClient.Builder webClient,
      ObjectMapper mapper,
      StreamBridge streamBridge,
      @Value("${app.product-service.host}") String productServiceHost,
      @Value("${app.product-service.port}") int productServicePort,

      @Value("${app.recommendation-service.host}") String recommendationServiceHost,
      @Value("${app.recommendation-service.port}") int recommendationServicePort,

      @Value("${app.review-service.host}") String reviewServiceHost,
      @Value("${app.review-service.port}") int reviewServicePort, Scheduler publishEventScheduler) {
    this.webClient = webClient.build();
    this.mapper = mapper;
    this.streamBridge = streamBridge;

    productServiceUrl = "http://" + productServiceHost + ":" + productServicePort;
    recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort;
    reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort;
    this.publishEventScheduler = publishEventScheduler;
  }

  @Override
  public Mono<Product> createProduct(Product body) {
    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Product> getProduct(int productId) {
    String url = productServiceUrl + "/product/" + productId;
    LOG.debug("This is he url: " + url);

    LOG.debug("Will call the getProduct API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToMono(Product.class)
        .log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, this::handleException);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    return Mono.fromRunnable(() -> sendMessage(
        "products-out-0", new Event(DELETE, productId, null)
    )).subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    String url = recommendationServiceUrl + "/recommendation?productId=" + productId;

    LOG.debug("Will call the getRecommendations API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToFlux(Recommendation.class)
        .log(LOG.getName(), FINE).onErrorResume(error -> empty());
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {
    return Mono.fromCallable(() -> {
      sendMessage("recommendations-out-0", new Event(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    return Mono.fromRunnable(() -> sendMessage("recommendations-out-0",
            new Event(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Mono<Review> createReview(Review body) {
    return Mono.fromCallable(() -> {
      sendMessage("reviews-out-0", new Event(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Flux<Review> getReviews(int productId) {

    String url = reviewServiceUrl + "/review?productId=" + productId;

    LOG.debug("Will call the getReviews API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToFlux(Review.class)
        .log(LOG.getName(), FINE).onErrorResume(error -> empty());
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {
    return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  private void sendMessage(String bindingName, Event event) {
    LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
    Message message = MessageBuilder.withPayload(event)
        .setHeader("partitionKey", event.getKey()).build();

    streamBridge.send(bindingName, message);
  }


  private Throwable handleException(Throwable ex) {
    if (!(ex instanceof WebClientResponseException)) {
      LOG.warn("Got an unexpected error: {}, will rethrow it", ex.toString());
      return ex;
    }

    WebClientResponseException wcre = (WebClientResponseException) ex;

    switch (wcre.getStatusCode()) {
      case NOT_FOUND:
        return new NotFoundException(getErrorMessage(wcre));

      case UNPROCESSABLE_ENTITY:
        return new InvalidInputException(getErrorMessage(wcre));

      default:
        LOG.warn("Got an unexpected HTTP error: {}", wcre.getStatusCode());
        LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
        return ex;
    }
  }

  private String getErrorMessage(WebClientResponseException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (IOException ioex) {
      return ex.getMessage();
    }
  }
}

