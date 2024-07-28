package com.kimsang.microservices.core.recommendation.services;

import static java.util.logging.Level.FINE;

import com.kimsang.api.core.recommendation.Recommendation;
import com.kimsang.api.core.recommendation.RecommendationService;
import com.kimsang.api.exceptions.InvalidInputException;
import com.kimsang.microservices.core.recommendation.persistence.RecommendationEntity;
import com.kimsang.microservices.core.recommendation.persistence.RecommendationRepository;
import com.kimsang.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);
  private final RecommendationRepository repository;
  private final RecommendationMapper mapper;
  private final ServiceUtil serviceUtil;

  @Autowired
  public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }

    RecommendationEntity entity = this.mapper.apiToEntity(body);
    return this.repository.save(entity)
        .log(LOG.getName(), FINE)
        .onErrorMap(DuplicateKeyException.class, ex ->
            new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", " + "Recommendation Id: " + body.getRecommendationId()))
        .map(mapper::entityToApi);
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get recommendations for product with id={}", productId);

    return repository.findByProductId(productId)
        .log(LOG.getName(), FINE)
        .map(mapper::entityToApi)
        .map(this::setServiceAddress);
  }


  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    return repository.deleteAll(repository.findByProductId(productId));
  }

  private Recommendation setServiceAddress(Recommendation e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
  }
}

