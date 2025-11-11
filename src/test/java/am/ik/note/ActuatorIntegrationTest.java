package am.ik.note;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.endpoint.health.probes.add-additional-paths=true", "management.server.port=28081",
				"management.endpoint.health.probes.enabled=true", "management.endpoints.web.exposure.include=*" })
@Testcontainers(disabledWithoutDocker = true)
public class ActuatorIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");

	@LocalServerPort
	int port;

	WebTestClient webTestClient;

	public ActuatorIntegrationTest() {
		this.webTestClient = WebTestClient.bindToServer(new JdkClientHttpConnector()).build();
	}

	@Test
	void infoIsAccessible() throws Exception {
		this.webTestClient.get()
			.uri("http://localhost:28081/actuator/info", this.port)
			.exchange()
			.expectStatus()
			.isOk();
	}

	@Test
	void livezIsAccessible() throws Exception {
		this.webTestClient.get().uri("http://localhost:{port}/livez", this.port).exchange().expectStatus().isOk();
	}

	@Test
	void readyzIsAccessible() throws Exception {
		this.webTestClient.get().uri("http://localhost:{port}/readyz", this.port).exchange().expectStatus().isOk();
	}

}
