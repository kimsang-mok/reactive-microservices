package com.kimsang.microservices.composite.product.services.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ObservationUtil {
  private final ObservationRegistry registry;

  public ObservationUtil(ObservationRegistry registry) {
    this.registry = registry;
  }

  public <T> T observe(String observationName, String contextualName, String highCardinalityKey,
                       String highCardinalityValue, Supplier<T> supplier) {
    return Observation.createNotStarted(observationName, registry)
        .contextualName(contextualName)
        .highCardinalityKeyValue(highCardinalityKey, highCardinalityValue)
        .observe(supplier);
  }
}
