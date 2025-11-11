package am.ik.note.content;

import java.util.List;
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
@Import({ NoteMapper.class, NoteReaderMapper.class, ReaderMapper.class })
@Testcontainers(disabledWithoutDocker = true)
class NoteMapperTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");

	@Autowired
	NoteMapper noteMapper;

	@Autowired
	NoteReaderMapper noteReaderMapper;

	@Autowired
	ReaderMapper readerMapper;

	@BeforeEach
	void init() {
		this.noteMapper.deleteByEntryId(100L);
		this.noteMapper.deleteByEntryId(200L);
	}

	@Test
	void insertAndFindByNoteId() {
		final NoteId noteId = NoteId.random();
		final int count = this.noteMapper.insertNote(noteId, 101L, "https://example.com");
		assertThat(count).isEqualTo(1);
		final Note note = this.noteMapper.findByNoteId(noteId).orElseThrow();
		assertThat(note.noteId()).isEqualTo(noteId);
		assertThat(note.entryId()).isEqualTo(101L);
		assertThat(note.noteUrl()).isEqualTo("https://example.com");
	}

	@Test
	void insetAndFindByEntryId() {
		final NoteId noteId = NoteId.random();
		final int count = this.noteMapper.insertNote(noteId, 101L, "https://example.com");
		assertThat(count).isEqualTo(1);
		final Note note = this.noteMapper.findByEntryId(101L).orElseThrow();
		assertThat(note.noteId()).isEqualTo(noteId);
		assertThat(note.entryId()).isEqualTo(101L);
		assertThat(note.noteUrl()).isEqualTo("https://example.com");
	}

	@Test
	void insetAndDeleteAndFindByEntryId() {
		final NoteId noteId = NoteId.random();
		final int count = this.noteMapper.insertNote(noteId, 101L, "https://example.com");
		assertThat(count).isEqualTo(1);
		final int deleted = this.noteMapper.deleteByEntryId(101L);
		assertThat(deleted).isEqualTo(1);
		final Optional<Note> note = this.noteMapper.findByEntryId(101L);
		assertThat(note.isEmpty()).isTrue();
	}

	@Test
	void findAll() {
		final NoteId noteId1 = NoteId.random();
		final NoteId noteId2 = NoteId.random();
		final NoteId noteId3 = NoteId.random();
		final NoteId noteId4 = NoteId.random();
		final ReaderId readerId1 = ReaderId.random();
		final ReaderId readerId2 = ReaderId.random();

		this.noteMapper.insertNote(noteId1, 101L, "https://example.com/note1");
		this.noteMapper.insertNote(noteId2, 102L, "https://example.com/note2");
		this.noteMapper.insertNote(noteId3, 103L, "https://example.com/note3");
		this.noteMapper.insertNote(noteId4, 104L, "https://example.com/note4");

		this.readerMapper.insert(readerId1, "reader1@example.com");
		this.readerMapper.insert(readerId2, "reader2@example.com");

		this.noteReaderMapper.insertNoteReader(noteId1, readerId1);
		this.noteReaderMapper.insertNoteReader(noteId2, readerId1);
		this.noteReaderMapper.insertNoteReader(noteId4, readerId1);
		this.noteReaderMapper.insertNoteReader(noteId1, readerId2);
		this.noteReaderMapper.insertNoteReader(noteId2, readerId2);
		this.noteReaderMapper.insertNoteReader(noteId3, readerId2);

		final List<NoteSummary> summaries1 = this.noteMapper.findAll(readerId1)
			.stream()
			.map(NoteSummaryBuilder::build)
			.toList();
		final List<NoteSummary> summaries2 = this.noteMapper.findAll(readerId2)
			.stream()
			.map(NoteSummaryBuilder::build)
			.toList();

		assertThat(summaries1.stream().map(NoteSummary::entryId)).containsExactly(101L, 102L, 103L, 104L);
		assertThat(summaries1.stream().map(NoteSummary::noteId)).containsExactly(noteId1, noteId2, noteId3, noteId4);
		assertThat(summaries1.stream().map(NoteSummary::subscribed)).containsExactly(true, true, false, true);

		assertThat(summaries2.stream().map(NoteSummary::entryId)).containsExactly(101L, 102L, 103L, 104L);
		assertThat(summaries2.stream().map(NoteSummary::noteId)).containsExactly(noteId1, noteId2, noteId3, noteId4);
		assertThat(summaries2.stream().map(NoteSummary::subscribed)).containsExactly(true, true, true, false);
	}

}