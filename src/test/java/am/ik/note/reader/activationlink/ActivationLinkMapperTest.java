package am.ik.note.reader.activationlink;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
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

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ ActivationLinkMapper.class, ReaderMapper.class })
@Testcontainers(disabledWithoutDocker = true)
class ActivationLinkMapperTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
			"postgres:14-alpine");

	@Autowired
	ActivationLinkMapper activationLinkMapper;

	@Autowired
	ReaderMapper readerMapper;

	ReaderId readerId = ReaderId.random();

	@BeforeEach
	void setup() {
		this.readerMapper.insert(readerId, "demo@example.com");
	}

	@Test
	void insertAndFind() {
		final ActivationLinkId activationLinkId = ActivationLinkId.random();
		final OffsetDateTime created = OffsetDateTime
				.parse("2022-12-09T10:58:12.445+09:00").toInstant()
				.atOffset(ZoneOffset.UTC);
		final int count = this.activationLinkMapper
				.insert(new ActivationLink(activationLinkId, readerId, created));
		assertThat(count).isEqualTo(1);
		final ActivationLink activationLink = this.activationLinkMapper
				.findById(activationLinkId).orElseThrow();
		assertThat(activationLink.activationId()).isEqualTo(activationLinkId);
		assertThat(activationLink.readerId()).isEqualTo(readerId);
		assertThat(activationLink.createdAt()).isEqualTo(created);
	}

	@Test
	void insertAndDeleteById() {
		final ActivationLinkId activationLinkId = ActivationLinkId.random();
		final OffsetDateTime created = OffsetDateTime
				.parse("2022-12-09T10:58:12.445+09:00").toInstant()
				.atOffset(ZoneOffset.UTC);
		final int count = this.activationLinkMapper
				.insert(new ActivationLink(activationLinkId, readerId, created));
		assertThat(count).isEqualTo(1);
		final int deleted = this.activationLinkMapper.deleteById(activationLinkId);
		assertThat(deleted).isEqualTo(1);
		final Optional<ActivationLink> activationLink = this.activationLinkMapper
				.findById(activationLinkId);
		assertThat(activationLink.isEmpty()).isTrue();
	}
}