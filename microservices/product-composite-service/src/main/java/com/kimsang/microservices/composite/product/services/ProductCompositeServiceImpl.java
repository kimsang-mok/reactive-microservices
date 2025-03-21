package com.kimsang.microservices.composite.product.services;


import static java.util.logging.Level.FINE;

import com.kimsang.api.composite.product.*;
import com.kimsang.api.core.product.Product;
import com.kimsang.api.core.recommendation.Recommendation;
import com.kimsang.api.core.review.Review;
import com.kimsang.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private final SecurityContextImpl nullSecCtx = new SecurityContextImpl();

  private final ServiceUtil serviceUtil;

  private ProductCompositeIntegration integration;

  @Autowired
  public ProductCompositeServiceImpl(
      ServiceUtil serviceUtil,
      ProductCompositeIntegration integration
  ) {
    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  public String sayHello() {
    LOG.debug("Hello world");
    return "Hello There 2";
  }

  @Override
  public Mono<Void> createProduct(ProductAggregate body) {
    try {
      List<Mono> monoList = new ArrayList<>();

      monoList.add(getLogAuthorizationInfoMono());

      LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

      Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
      monoList.add(integration.createProduct(product));

      if (body.getRecommendations() != null) {
        body.getRecommendations().forEach(r -> {
          Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(),
              r.getAuthor(), r.getRate(), r.getContent(), null);
          monoList.add(integration.createRecommendation(recommendation));
        });
      }

      if (body.getReviews() != null) {
        body.getReviews().forEach(r -> {
          Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(),
              r.getContent(), null);
          monoList.add(integration.createReview(review));
        });
      }
      LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

      return Mono.zip(r -> "", monoList.toArray(new Mono[0]))
          .doOnError(ex -> LOG.warn("createCompositeProduct failed: {}", ex.toString()))
          .then();

    } catch (RuntimeException re) {
      LOG.warn("createCompositeProduct failed: {}", re.toString());
      throw re;
    }
  }

  @Override
  public Mono<ProductAggregate> getProduct(int productId) {

    if (productId == -1) {
      LOG.warn("Invalid productId: {}", productId);
      return Mono.error(new RuntimeException("No product found"));
    }

    LOG.info("Will get composite product info for product.id: {}", productId);
    return Mono.zip(
            values -> createProductAggregate(
                (SecurityContext) values[0],
                (Product) values[1], (List<Recommendation>) values[2],
                (List<Review>) values[3], serviceUtil.getServiceAddress()),
            integration.getProduct(productId),
            integration.getRecommendations(productId).collectList(),
            integration.getReviews(productId).collectList()
        ).doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
        .log(LOG.getName(), FINE);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    try {
      LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);
      return Mono.zip(
              r -> "",
              getLogAuthorizationInfoMono(),
              integration.deleteProduct(productId),
              integration.deleteRecommendations(productId),
              integration.deleteReviews(productId)
          ).doOnError(ex -> LOG.warn("delete failed: {}", ex.toString()))
          .log(LOG.getName(), FINE).then();
    } catch (RuntimeException re) {
      LOG.warn("deleteCompositeProduct failed: {}", re.toString());
      throw re;
    }
  }

  private ProductAggregate createProductAggregate(
      SecurityContext sc,
      Product product,
      List<Recommendation> recommendations,
      List<Review> reviews,
      String serviceAddress) {
    logAuthorizationInfo(sc);

    // 1. Setup product info
    int productId = product.getProductId();
    String name = product.getName();
    int weight = product.getWeight();

    // 2. Copy summary recommendation info, if available
    List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
        recommendations.stream().map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(),
            r.getRate(), r.getContent())).toList();

    // 3. Copy summary review info, if available
    List<ReviewSummary> reviewSummaries = (reviews == null) ? null :
        reviews.stream()
            .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
            .toList();


    // 4. Create info regarding the involved microservices addresses
    String productAddress = product.getServiceAddress();
    String reviewAddress = (reviews != null && !reviews.isEmpty()) ? reviews.getFirst().getServiceAddress() : "";
    String recommendationAddress = (recommendations != null && !recommendations.isEmpty()) ?
        recommendations.getFirst().getServiceAddress() : "";
    ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress,
        recommendationAddress);

    return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
  }

  private Mono<SecurityContext> getLogAuthorizationInfoMono() {
    return getSecurityContextMono().doOnNext(this::logAuthorizationInfo);
  }

  private Mono<SecurityContext> getSecurityContextMono() {
    return ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSecCtx);
  }

  private void logAuthorizationInfo(SecurityContext sc) {
    if (sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken) {
      Jwt jwtToken = ((JwtAuthenticationToken) sc.getAuthentication()).getToken();
      logAuthorizationInfo(jwtToken);
    } else {
      LOG.warn("No JWT based authentication supplied. Is running test.");
    }
  }

  private void logAuthorizationInfo(Jwt jwt) {
    if (jwt == null) {
      LOG.warn("No JWT supplied. Is running test.");
    } else {
      URL issuer = jwt.getIssuer();
      List<String> audience = jwt.getAudience();
      Object subject = jwt.getClaims().get("sub");
      Object scopes = jwt.getClaims().get("scope");
      Object expires = jwt.getClaims().get("exp");

      LOG.debug("Authorization info: Subject: {}, scopes: {}, expires: {}, issuer: {}, audience: {}", subject, scopes,
          expires, issuer, audience);
    }
  }
}
