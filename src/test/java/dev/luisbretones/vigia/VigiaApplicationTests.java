package dev.luisbretones.vigia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "vigia.config=src/test/resources/checks-test.yml")
class VigiaApplicationTests {

	@Test
	void contextLoads() {
	}

}
