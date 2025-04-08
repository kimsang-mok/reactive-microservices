package com.kimsang.microservices.composite.product;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestSecurityConfig {
  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) throws Exception {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(auth -> auth.anyExchange().permitAll());
    return http.build();
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    return token -> Mono.empty();
  }
}
