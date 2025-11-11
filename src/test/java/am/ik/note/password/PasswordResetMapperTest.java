package am.ik.note.password;

import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({ ReaderMapper.class, PasswordResetMapper.class })
@Testcontainers(disabledWithoutDocker = true)
class PasswordResetMapperTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");

	@Autowired
	PasswordResetMapper passwordResetMapper;

	@Autowired
	ReaderMapper readerMapper;

	@BeforeEach
	void setup() {
		final ReaderId readerId = ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4");
		this.readerMapper.insert(readerId, "demo@example.com");
	}

	@Test
	void insertAndFind() {
		final PasswordResetId resetId = PasswordResetId.random();
		final ReaderId readerId = ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4");
		final int count = this.passwordResetMapper.insert(resetId, readerId);
		assertThat(count).isEqualTo(1);
		final PasswordReset passwordReset = this.passwordResetMapper.findByResetId(resetId).orElseThrow();
		assertThat(passwordReset.resetId()).isEqualTo(resetId);
		assertThat(passwordReset.readerId()).isEqualTo(readerId);
	}

	@Test
	void insertAndDeleteAndFind() {
		final PasswordResetId resetId = PasswordResetId.random();
		final ReaderId readerId = ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4");
		final int count = this.passwordResetMapper.insert(resetId, readerId);
		assertThat(count).isEqualTo(1);
		final int deleted = this.passwordResetMapper.deleteByResetId(resetId);
		assertThat(deleted).isEqualTo(1);
		final Optional<PasswordReset> passwordReset = this.passwordResetMapper.findByResetId(resetId);
		assertThat(passwordReset.isEmpty()).isTrue();
	}

}