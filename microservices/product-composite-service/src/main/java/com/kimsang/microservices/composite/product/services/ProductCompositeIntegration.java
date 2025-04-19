package com.kimsang.microservices.composite.product.services;


import static java.util.logging.Level.FINE;
import static reactor.core.publisher.Flux.empty;
import static com.kimsang.api.event.Event.Type.CREATE;
import static com.kimsang.api.event.Event.Type.DELETE;


import com.kimsang.util.http.ServiceUtil;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.kimsang.util.http.HttpErrorInfo;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.net.URI;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private static final String PRODUCT_SERVICE_URL = "http://product";
  private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
  private static final String REVIEW_SERVICE_URL = "http://review";

  private final WebClient webClient;
  private final ObjectMapper mapper;

  private final StreamBridge streamBridge;
  private final Scheduler publishEventScheduler;
  public final ServiceUtil serviceUtil;


  @Autowired
  public ProductCompositeIntegration(
      WebClient webClient,
      ObjectMapper mapper,
      StreamBridge streamBridge,
      @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
      ServiceUtil serviceUtil
  ) {
    this.webClient = webClient;
    this.mapper = mapper;
    this.streamBridge = streamBridge;
    this.publishEventScheduler = publishEventScheduler;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Product> createProduct(Product body) {
    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  @Retry(name = "product")
  @TimeLimiter(name = "product")
  @CircuitBreaker(name = "product", fallbackMethod = "getProductFallbackValue")
  public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
    URI url = UriComponentsBuilder.fromUriString(PRODUCT_SERVICE_URL + "/product/{productId}?delay={delay" +
        "}&faultPercent={faultPercent}").build(productId, delay, faultPercent);

    LOG.debug("This is the url: " + url);

    LOG.debug("Will call the getProduct API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToMono(Product.class)
        .log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, this::handleException);
  }

  public Mono<Product> getProductFallbackValue(int productId, int delay, int faultPercent,
                                               CallNotPermittedException ex) {
    LOG.warn("Creating a fail-fast fallback product for productId {}, delay = {}, faultPercent = {} and exception = {}",
        productId, delay, faultPercent, ex.toString());

    if (productId == 13) {
      String errMsg = "Product Id: " + productId + " not found in fallback cache!";
      LOG.warn(errMsg);
      throw new NotFoundException(errMsg);
    }

    return Mono.just(new

        Product(productId, "Fallback product" + productId, productId,
        serviceUtil.getServiceAddress()));
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    return Mono.fromRunnable(() -> sendMessage(
        "products-out-0", new Event(DELETE, productId, null)
    )).subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    URI url =
        UriComponentsBuilder.fromUriString(RECOMMENDATION_SERVICE_URL + "/recommendation?productId={productId}").build(productId);

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
    URI url = UriComponentsBuilder.fromUriString(REVIEW_SERVICE_URL + "/review?productId={productId}").build(productId);

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


  public Mono<Health> getProductHealth() {
    return getHealth(PRODUCT_SERVICE_URL);
  }

  public Mono<Health> getRecommendationHealth() {
    return getHealth(RECOMMENDATION_SERVICE_URL);
  }

  public Mono<Health> getReviewHealth() {
    return getHealth(REVIEW_SERVICE_URL);
  }

  private Mono<Health> getHealth(String url) {
    url += "/actuator/health";
    LOG.debug("Will call the Health API on URL: {}", url);
    return webClient.get().uri(url).retrieve().bodyToMono(String.class)
        /**
         * If there's an error (e.g. the service is down or the request fails), this creates health object that
         * indicates the service is "down" and includes the exception(ex) that caused the failure
         */
        .map(s -> new Health.Builder().up().build())
        .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
        .log(LOG.getName(), FINE);
  }
}

