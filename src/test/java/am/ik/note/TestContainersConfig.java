package am.ik.note;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

@TestConfiguration
public class TestContainersConfig {

	@Bean
	@ServiceConnection
	public PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>("postgres:14-alpine");
	}

	@Bean
	FixedHostPortGenericContainer<?> sendgrid(@Value("${maildev.port:0}") int maildevPort) {
		var container = new FixedHostPortGenericContainer<>("ykanazawa/sendgrid-maildev")
			.withEnv("SENDGRID_DEV_API_SERVER", ":3030")
			.withEnv("SENDGRID_DEV_API_KEY", "SG.test")
			.withEnv("SENDGRID_DEV_SMTP_SERVER", "127.0.0.1:1025")
			.withExposedPorts(3030, 1080)
			.waitingFor(new LogMessageWaitStrategy().withRegEx(".*sendgrid-dev entered RUNNING state.*")
				.withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS)))
			.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("sendgrid-maildev")));
		return maildevPort > 0 ? container.withFixedExposedPort(maildevPort, 1080) : container;
	}

	@Bean
	DynamicPropertyRegistrar dynamicPropertyRegistrar(GenericContainer<?> sendgrid) {
		return registry -> {
			registry.add("spring.sendgrid.base-url", () -> "http://127.0.0.1:" + sendgrid.getMappedPort(3030));
			registry.add("spring.sendgrid.api-key", () -> "SG.test");
			registry.add("maildev.port", () -> sendgrid.getMappedPort(1080));
		};
	}

}
