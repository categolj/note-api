package am.ik.note.reader;

import java.util.List;
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
@Import({ ReaderMapper.class })
@Testcontainers(disabledWithoutDocker = true)
class ReaderMapperTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");

	@Autowired
	ReaderMapper readerMapper;

	@Test
	void insertAndFindAll() throws Exception {
		this.readerMapper.insert(ReaderId.random(), "demo1@example.com");
		this.readerMapper.insert(ReaderId.random(), "demo2@example.com");
		this.readerMapper.insert(ReaderId.random(), "demo3@example.com");
		this.readerMapper.insert(ReaderId.random(), "demo4@example.com");
		this.readerMapper.insert(ReaderId.random(), "demo5@example.com");
		this.readerMapper.insert(ReaderId.random(), "demo6@example.com");
		final List<Reader> readers = readerMapper.findAll();
		assertThat(readers.stream().map(Reader::email).toList()).containsExactlyInAnyOrder("demo6@example.com",
				"demo5@example.com", "demo4@example.com", "demo3@example.com", "demo2@example.com",
				"demo1@example.com");
	}

	@Test
	void insertAndFindByEmail() {
		final ReaderId readerId = ReaderId.random();
		final int count = this.readerMapper.insert(readerId, "demo@example.com");
		assertThat(count).isEqualTo(1);
		final Reader reader = this.readerMapper.findByEmail("demo@example.com").orElseThrow();
		assertThat(reader.readerId()).isEqualTo(readerId);
		assertThat(reader.email()).isEqualTo("demo@example.com");
		assertThat(reader.isDisabled()).isTrue();
		assertThat(reader.hashedPassword()).isNull();
	}

	@Test
	void insertAndFindById() {
		final ReaderId readerId = ReaderId.random();
		final int count = this.readerMapper.insert(readerId, "demo@example.com");
		assertThat(count).isEqualTo(1);
		final Reader reader = this.readerMapper.findById(readerId).orElseThrow();
		assertThat(reader.readerId()).isEqualTo(readerId);
		assertThat(reader.email()).isEqualTo("demo@example.com");
		assertThat(reader.isDisabled()).isTrue();
		assertThat(reader.hashedPassword()).isNull();
	}

	@Test
	void insertAndUpdateReaderStateAndFindByEmail() {
		final ReaderId readerId = ReaderId.random();
		final int count = this.readerMapper.insert(readerId, "demo@example.com");
		assertThat(count).isEqualTo(1);
		final int updated = this.readerMapper.updateReaderState(readerId, ReaderState.ENABLED);
		assertThat(updated).isEqualTo(1);
		final Reader reader = this.readerMapper.findByEmail("demo@example.com").orElseThrow();
		assertThat(reader.readerId()).isEqualTo(readerId);
		assertThat(reader.email()).isEqualTo("demo@example.com");
		assertThat(reader.readerState()).isEqualTo(ReaderState.ENABLED);
		assertThat(reader.hashedPassword()).isNull();
	}

}