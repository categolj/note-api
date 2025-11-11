package am.ik.note.content;

import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
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
@Import({ NoteMapper.class, NoteReaderMapper.class, ReaderMapper.class })
@Testcontainers(disabledWithoutDocker = true)
class NoteReaderMapperTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");

	@Autowired
	NoteMapper noteMapper;

	@Autowired
	NoteReaderMapper noteReaderMapper;

	@Autowired
	ReaderMapper readerMapper;

	@Test
	void insertAndCount() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		this.noteMapper.insertNote(noteId, 101L, "https://example.com/note1");
		this.readerMapper.insert(readerId, "reader1@example.com");
		this.noteReaderMapper.insertNoteReader(noteId, readerId);
		final int count = this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void insertAndCount_notFound() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		this.noteMapper.insertNote(noteId, 101L, "https://example.com/note1");
		this.readerMapper.insert(readerId, "reader1@example.com");
		final int count = this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId);
		assertThat(count).isEqualTo(0);
	}

}