package com.kimsang.springcloud.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class GatewayApplication {

  @Bean
  @LoadBalanced
  public WebClient.Builder loadBalanceWebClientBuilder() {
    return WebClient.builder();
  }

  private static final Logger LOG = LoggerFactory.getLogger(GatewayApplication.class);

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    ConfigurableApplicationContext ctx = SpringApplication.run(GatewayApplication.class, args);
    LOG.info("Eureka server: {}", ctx.getEnvironment().getProperty("app.eureka-server"));
  }
}
