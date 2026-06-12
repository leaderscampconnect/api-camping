package com.esprit.microservice.apicamping;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Disabled because it requires a live PostgreSQL database to boot the application context, which is not available in standard CI pipelines.")
class ApiCampingApplicationTests {

	@Test
	void contextLoads() {
	}

}
