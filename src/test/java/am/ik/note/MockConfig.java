package am.ik.note;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class MockConfig {
	@Bean
	public MockIdGenerator mockIdGenerator() {
		return new MockIdGenerator();
	}

	@Bean
	public Clock clock() {
		return Clock.fixed(Instant.parse("2022-12-06T06:38:31.343307Z"), ZoneId.of("UTC"));
	}

	@Bean
	@SuppressWarnings("deprecation")
	public PasswordEncoder passwordEncoder() {
		return new DelegatingPasswordEncoder("noop", Map.of("noop", NoOpPasswordEncoder.getInstance()));
	}
}
