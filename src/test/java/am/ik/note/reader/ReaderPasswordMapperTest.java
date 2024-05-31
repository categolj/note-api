package am.ik.note.reader;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(properties = "logging.level.sql=INFO")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ ReaderMapper.class, ReaderPasswordMapper.class })
@Testcontainers(disabledWithoutDocker = true)
class ReaderPasswordMapperTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
			"postgres:14-alpine");

	@Autowired
	ReaderMapper readerMapper;

	@Autowired
	ReaderPasswordMapper readerPasswordMapper;

	@BeforeEach
	void setup() {
		final ReaderId readerId = ReaderId
				.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4");
		this.readerMapper.insert(readerId, "demo@example.com");
	}

	@Test
	void insertAndFind() {
		final ReaderId readerId = ReaderId
				.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4");
		final int count = this.readerPasswordMapper
				.insert(new ReaderPassword(readerId, "{noop}password"));
		assertThat(count).isEqualTo(1);
		final Reader reader = this.readerMapper.findByEmail("demo@example.com")
				.orElseThrow();
		assertThat(reader.readerId()).isEqualTo(readerId);
		assertThat(reader.email()).isEqualTo("demo@example.com");
		assertThat(reader.isDisabled()).isTrue();
		assertThat(reader.hashedPassword()).isEqualTo("{noop}password");
	}

	@Test
	void insertAndDeleteAndFind() {
		final ReaderId readerId = ReaderId
				.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4");
		final int count = this.readerPasswordMapper
				.insert(new ReaderPassword(readerId, "{noop}password"));
		assertThat(count).isEqualTo(1);
		final int deleted = this.readerPasswordMapper.deleteByReaderId(readerId);
		assertThat(deleted).isEqualTo(1);
		final Reader reader = this.readerMapper.findByEmail("demo@example.com")
				.orElseThrow();
		assertThat(reader.readerId()).isEqualTo(readerId);
		assertThat(reader.email()).isEqualTo("demo@example.com");
		assertThat(reader.isDisabled()).isTrue();
		assertThat(reader.hashedPassword()).isNull();
	}
}