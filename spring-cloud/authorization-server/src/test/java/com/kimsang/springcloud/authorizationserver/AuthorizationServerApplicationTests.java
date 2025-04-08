package com.kimsang.springcloud.authorizationserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.config.enabled=false"})
class AuthorizationServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
