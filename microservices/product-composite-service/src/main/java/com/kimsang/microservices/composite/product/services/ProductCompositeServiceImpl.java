package com.kimsang.microservices.composite.product.services;

import com.kimsang.api.composite.ProductAggregate;
import com.kimsang.api.composite.ProductCompositeService;
import com.kimsang.api.composite.ServiceAddresses;
import com.kimsang.api.core.product.Product;
import com.kimsang.api.exceptions.NotFoundException;
import com.kimsang.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

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
    LOG.error("Hello world");
    return "Hello";
  }

  @Override
  public void createProduct(ProductAggregate body) {
    try {
      LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

      Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
      integration.createProduct(product);

      LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());
    } catch (RuntimeException re) {
      LOG.warn("createCompositeProduct failed", re);
      throw re;
    }
  }

  @Override
  public ProductAggregate getProduct(int productId) {

    LOG.debug("getCompositeProduct: lookup a product aggregate for productId: {}", productId);

    Product product = integration.getProduct(productId);
    if (product == null) {
      throw new NotFoundException("No product found for productId: " + productId);
    }


    LOG.debug("getCompositeProduct: aggregate entity found for productId: {}", productId);

    return createProductAggregate(product, serviceUtil.getServiceAddress());
  }

  private ProductAggregate createProductAggregate(
      Product product,
      String serviceAddress) {

    // 1. Setup product info
    int productId = product.getProductId();
    String name = product.getName();
    int weight = product.getWeight();


    // 4. Create info regarding the involved microservices addresses
    String productAddress = product.getServiceAddress();
    ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress);

    return new ProductAggregate(productId, name, weight, serviceAddresses);
  }
}
