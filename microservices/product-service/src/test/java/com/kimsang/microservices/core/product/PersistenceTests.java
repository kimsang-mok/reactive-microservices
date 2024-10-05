package com.kimsang.microservices.core.product;

import com.kimsang.microservices.core.product.persistence.ProductEntity;
import com.kimsang.microservices.core.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;

@DataMongoTest
class PersistenceTests extends MongoDbTestBase {
  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();

    ProductEntity entity = new ProductEntity(1, "n", 1);
    StepVerifier.create(repository.save(entity))
        .expectNextMatches(createdEntity -> {
          savedEntity = createdEntity;
          return areProductEqual(entity, savedEntity);
        }).verifyComplete();
  }

  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);

    StepVerifier.create(repository.save(newEntity)).expectNextMatches(createdEntity ->
        newEntity.getProductId() == createdEntity.getProductId()).verifyComplete();

    StepVerifier.create(repository.findById(newEntity.getId()))
        .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity)).verifyComplete();

    StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    StepVerifier.create(repository.save(savedEntity)).expectNextMatches(updatedEntity ->
        updatedEntity.getName().equals("n2")).verifyComplete();

    StepVerifier.create(repository.findById(savedEntity.getId()))
        .expectNextMatches(foundEntity ->
            foundEntity.getVersion().equals(1) && foundEntity.getName().equals("n2")).verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
    StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
  }

  @Test
  void getByProductId() {
    StepVerifier.create(repository.findByProductId(savedEntity.getProductId())).expectNextMatches(foundEntity ->
        areProductEqual(savedEntity, foundEntity)).verifyComplete();
  }

  @Test
  void duplicateError() {
    ProductEntity entity = new ProductEntity(1, "n", 1);
    StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
  }

  @Test
  void optimisticLockError() {
    // store the saved entity in two separate entity objects
    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

    // update the entity using the first entity object
    if (entity1 != null) {
      entity1.setName("n1");
      repository.save(entity1).block();
    }

    // update the entity using the second entity object
    // this should fail since the second entity now holds an old version number
    if (entity2 != null) {
      entity2.setName("n2");
      StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();
    }

    // get the updated entity from the database and verify its new state
    StepVerifier.create(repository.findById(savedEntity.getId())).expectNextMatches(foundEntity ->
        foundEntity.getVersion().equals(1) && foundEntity.getName().equals("n1")).verifyComplete();
  }

  /*
  @Test
  void checkMongoVersion() {
    String version = mongoTemplate.executeCommand("{ buildInfo: 1 }")
        .get("version")
        .toString();
    assertEquals("4.4.2", version);
  }
  */

  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return (expectedEntity.getId().equals(actualEntity.getId()))
        && (expectedEntity.getVersion() == actualEntity.getVersion())
        && (expectedEntity.getProductId() == actualEntity.getProductId())
        && (expectedEntity.getName().equals(actualEntity.getName()))
        && (expectedEntity.getWeight() == actualEntity.getWeight());

  }
}
