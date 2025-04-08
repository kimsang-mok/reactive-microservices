package com.kimsang.springcloud.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		properties = {"spring.cloud.config.enabled=false"}
)
class EurekaServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
