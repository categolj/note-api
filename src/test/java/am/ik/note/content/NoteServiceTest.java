package am.ik.note.content;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import am.ik.note.MockConfig;
import am.ik.note.content.NoteService.SubscriptionStatus;
import am.ik.note.entry.Author;
import am.ik.note.entry.Entries;
import am.ik.note.entry.Entry;
import am.ik.note.entry.EntryClient;
import am.ik.note.entry.FrontMatter;
import am.ik.note.reader.ReaderId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith({ SpringExtension.class, OutputCaptureExtension.class })
@Import({ NoteService.class, MockConfig.class })
class NoteServiceTest {

	@MockBean
	NoteMapper noteMapper;

	@MockBean
	NoteReaderMapper noteReaderMapper;

	@MockBean
	EntryClient entryClient;

	@Autowired
	NoteService noteService;

	@Test
	void findAll() {
		final ReaderId readerId = ReaderId.random();
		final NoteId noteId1 = NoteId.random();
		final NoteId noteId2 = NoteId.random();
		given(this.noteMapper.findAll(readerId)).willReturn(List.of(
				new NoteSummaryBuilder().withNoteId(noteId1)
					.withEntryId(101L)
					.withNoteUrl("https://example.com/note1")
					.withSubscribed(true),
				new NoteSummaryBuilder().withNoteId(noteId2)
					.withEntryId(102L)
					.withNoteUrl("https://example.com/note2")
					.withSubscribed(false)));
		given(this.entryClient.getEntries()).willReturn(new Entries(List.of(
				new Entry(101L, new FrontMatter("title1"), null, null, new Author("admin", OffsetDateTime.now())),
				new Entry(102L, new FrontMatter("title2"), null, null, new Author("admin", OffsetDateTime.now())))));
		final List<NoteSummary> summaries = this.noteService.findAll(readerId);

		assertThat(summaries.stream().map(NoteSummary::title)).containsExactly("title1", "title2");
		assertThat(summaries.stream().map(NoteSummary::entryId)).containsExactly(101L, 102L);
		assertThat(summaries.stream().map(NoteSummary::noteUrl)).containsExactly("https://example.com/note1",
				"https://example.com/note2");
		assertThat(summaries.stream().map(NoteSummary::subscribed)).containsExactly(true, false);
	}

	@Test
	void findByEntryId() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteMapper.findByEntryId(100L))
			.willReturn(Optional.of(new Note(noteId, 100L, "https://example.com")));
		given(this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId)).willReturn(1);
		given(this.entryClient.getEntry(100L)).willReturn(new Entry(100L, new FrontMatter("Hello World!"), "hello",
				new Author("demo1", OffsetDateTime.MIN), new Author("demo2", OffsetDateTime.MAX)));
		final NoteDetails noteDetails = this.noteService.findByEntryId(100L, readerId).orElseThrow();
		assertThat(noteDetails.noteId()).isEqualTo(noteId);
		assertThat(noteDetails.entryId()).isEqualTo(100L);
		assertThat(noteDetails.noteUrl()).isEqualTo("https://example.com");
		assertThat(noteDetails.content()).isEqualTo("hello");
		assertThat(noteDetails.frontMatter()).isNotNull();
		assertThat(noteDetails.frontMatter().title()).isEqualTo("Hello World!");
		assertThat(noteDetails.created()).isNotNull();
		assertThat(noteDetails.created().name()).isEqualTo("demo1");
		assertThat(noteDetails.created().date()).isEqualTo(OffsetDateTime.MIN);
		assertThat(noteDetails.updated()).isNotNull();
		assertThat(noteDetails.updated().name()).isEqualTo("demo2");
		assertThat(noteDetails.updated().date()).isEqualTo(OffsetDateTime.MAX);
	}

	@Test
	void findByEntryId_notSubscribed() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteMapper.findByEntryId(100L))
			.willReturn(Optional.of(new Note(noteId, 100L, "https://example.com")));
		given(this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId)).willReturn(0);
		assertThatThrownBy(() -> this.noteService.findByEntryId(100L, readerId))
			.isInstanceOf(NoteNotSubscribedException.class);
	}

	@Test
	void findByNoteId() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteMapper.findByNoteId(noteId))
			.willReturn(Optional.of(new Note(noteId, 100L, "https://example.com")));
		given(this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId)).willReturn(1);
		given(this.entryClient.getEntry(100L)).willReturn(new Entry(100L, new FrontMatter("Hello World!"), "hello",
				new Author("demo1", OffsetDateTime.MIN), new Author("demo2", OffsetDateTime.MAX)));
		final NoteDetails noteDetails = this.noteService.findByNoteId(noteId, readerId).orElseThrow();
		assertThat(noteDetails.noteId()).isEqualTo(noteId);
		assertThat(noteDetails.entryId()).isEqualTo(100L);
		assertThat(noteDetails.noteUrl()).isEqualTo("https://example.com");
		assertThat(noteDetails.content()).isEqualTo("hello");
		assertThat(noteDetails.frontMatter()).isNotNull();
		assertThat(noteDetails.frontMatter().title()).isEqualTo("Hello World!");
		assertThat(noteDetails.created()).isNotNull();
		assertThat(noteDetails.created().name()).isEqualTo("demo1");
		assertThat(noteDetails.created().date()).isEqualTo(OffsetDateTime.MIN);
		assertThat(noteDetails.updated()).isNotNull();
		assertThat(noteDetails.updated().name()).isEqualTo("demo2");
		assertThat(noteDetails.updated().date()).isEqualTo(OffsetDateTime.MAX);
	}

	@Test
	void findByNoteId_notSubscribed() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteMapper.findByNoteId(noteId))
			.willReturn(Optional.of(new Note(noteId, 100L, "https://example.com")));
		given(this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId)).willReturn(0);
		assertThatThrownBy(() -> this.noteService.findByNoteId(noteId, readerId))
			.isInstanceOf(NoteNotSubscribedException.class);
	}

	@Test
	void subscribe() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId)).willReturn(0);
		final SubscriptionStatus status = this.noteService.subscribe(noteId, readerId);
		assertThat(status).isEqualTo(SubscriptionStatus.NEW);
	}

	@Test
	void subscribeExisting() {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId)).willReturn(1);
		final SubscriptionStatus status = this.noteService.subscribe(noteId, readerId);
		assertThat(status).isEqualTo(SubscriptionStatus.EXISTING);
	}

}