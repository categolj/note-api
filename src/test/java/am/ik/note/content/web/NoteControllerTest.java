package am.ik.note.content.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import am.ik.note.config.SecurityConfig;
import am.ik.note.content.Note;
import am.ik.note.content.NoteDetails;
import am.ik.note.content.NoteId;
import am.ik.note.content.NoteMapper;
import am.ik.note.content.NoteNotSubscribedException;
import am.ik.note.content.NoteService;
import am.ik.note.content.NoteService.SubscriptionStatus;
import am.ik.note.content.NoteSummaryBuilder;
import am.ik.note.entry.Author;
import am.ik.note.entry.FrontMatter;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoteController.class)
@Import(SecurityConfig.class)
class NoteControllerTest {
	@MockBean
	NoteService noteService;

	@MockBean
	NoteMapper noteMapper;

	@MockBean
	ReaderMapper readerMapper;

	@Autowired
	MockMvc mockMvc;

	@Test
	void getNotes_200() throws Exception {
		final ReaderId readerId = ReaderId.random();
		final NoteId noteId1 = NoteId.random();
		final NoteId noteId2 = NoteId.random();
		given(this.noteService.findAll(readerId)).willReturn(List.of(
				new NoteSummaryBuilder().withNoteId(noteId1).withEntryId(101L)
						.withTitle("title1").withNoteUrl("https://example.com/note1")
						.withSubscribed(true).build(),
				new NoteSummaryBuilder().withNoteId(noteId2).withEntryId(102L)
						.withTitle("title2").withNoteUrl("https://example.com/note2")
						.withSubscribed(false).build()));
		this.mockMvc
				.perform(get("/notes")
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].noteId").doesNotExist())
				.andExpect(jsonPath("$[0].entryId").value(101L))
				.andExpect(jsonPath("$[0].title").value("title1"))
				.andExpect(jsonPath("$[0].noteUrl").value("https://example.com/note1"))
				.andExpect(jsonPath("$[0].subscribed").value(true))
				.andExpect(jsonPath("$[1].noteId").doesNotExist())
				.andExpect(jsonPath("$[1].entryId").value(102L))
				.andExpect(jsonPath("$[1].title").value("title2"))
				.andExpect(jsonPath("$[1].noteUrl").value("https://example.com/note2"))
				.andExpect(jsonPath("$[1].subscribed").value(false));
	}

	@Test
	void getNoteByEntryId_200() throws Exception {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteService.findByEntryId(100L, readerId)).willReturn(Optional.of(
				new NoteDetails(noteId, 100L, "hello", new FrontMatter("Hello World!"),
						"https://example.com", new Author("demo1", OffsetDateTime.MIN),
						new Author("demo2", OffsetDateTime.MAX))));
		this.mockMvc
				.perform(get("/notes/100")
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.noteId").doesNotExist())
				.andExpect(jsonPath("$.entryId").value(100L))
				.andExpect(jsonPath("$.content").value("hello"))
				.andExpect(jsonPath("$.frontMatter.title").value("Hello World!"))
				.andExpect(jsonPath("$.noteUrl").value("https://example.com"))
				.andExpect(jsonPath("$.created.name").value("demo1"))
				.andExpect(jsonPath("$.created.date").isNotEmpty())
				.andExpect(jsonPath("$.updated.name").value("demo2"))
				.andExpect(jsonPath("$.updated.date").isNotEmpty());
	}

	@Test
	void getNoteByEntryId_404() throws Exception {
		final ReaderId readerId = ReaderId.random();
		given(this.noteService.findByEntryId(100L, readerId))
				.willReturn(Optional.empty());
		this.mockMvc
				.perform(get("/notes/100")
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isNotFound());
	}

	@Test
	void getNoteByEntryId_403() throws Exception {
		final ReaderId readerId = ReaderId.random();
		given(this.noteService.findByEntryId(100L, readerId))
				.willThrow(new NoteNotSubscribedException("abc", "https://example.com"));
		this.mockMvc
				.perform(get("/notes/100")
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("abc"))
				.andExpect(jsonPath("$.noteUrl").value("https://example.com"));
	}

	@Test
	void getNoteByNoteId_200() throws Exception {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteService.findByNoteId(noteId, readerId)).willReturn(Optional.of(
				new NoteDetails(noteId, 100L, "hello", new FrontMatter("Hello World!"),
						"https://example.com", new Author("demo1", OffsetDateTime.MIN),
						new Author("demo2", OffsetDateTime.MAX))));
		this.mockMvc
				.perform(get("/notes/{noteId}", noteId)
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.noteId").value(noteId.toString()))
				.andExpect(jsonPath("$.entryId").value(100L))
				.andExpect(jsonPath("$.content").value("hello"))
				.andExpect(jsonPath("$.frontMatter.title").value("Hello World!"))
				.andExpect(jsonPath("$.noteUrl").value("https://example.com"))
				.andExpect(jsonPath("$.created.name").value("demo1"))
				.andExpect(jsonPath("$.created.date").isNotEmpty())
				.andExpect(jsonPath("$.updated.name").value("demo2"))
				.andExpect(jsonPath("$.updated.date").isNotEmpty());
	}

	@Test
	void getNoteByNoteId_404() throws Exception {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteService.findByNoteId(noteId, readerId))
				.willReturn(Optional.empty());
		this.mockMvc
				.perform(get("/notes/{noteId}", noteId)
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isNotFound());
	}

	@Test
	void getNoteByNoteId_403() throws Exception {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteService.findByNoteId(noteId, readerId))
				.willThrow(new NoteNotSubscribedException("abc", "https://example.com"));
		this.mockMvc
				.perform(get("/notes/{noteId}", noteId)
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("abc"))
				.andExpect(jsonPath("$.noteUrl").value("https://example.com"));
	}

	@Test
	void deleteByEntryId() throws Exception {
		given(this.noteMapper.deleteByEntryId(100L)).willReturn(1);
		this.mockMvc
				.perform(delete("/notes?entryId=100").with(jwt()
						.jwt(jwt -> jwt.claim("scope", List.of("note:admin")).build())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("deleted (1)"));
	}

	@Test
	void putNote() throws Exception {
		final NoteId noteId = NoteId.random();
		this.mockMvc.perform(put("/notes/100")
				.with(jwt().jwt(jwt -> jwt.claim("scope", List.of("note:admin")).build()))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"noteId": "%s", "noteUrl": "https://example.com"}
						""".formatted(noteId))).andExpect(status().isOk());
	}

	@Test
	void subscribe_200() throws Exception {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteMapper.findByNoteId(noteId))
				.willReturn(Optional.of(new Note(noteId, 100L, "https://example.com")));
		given(this.noteService.subscribe(noteId, readerId))
				.willReturn(SubscriptionStatus.NEW);
		this.mockMvc
				.perform(post("/notes/{noteId}/subscribe", noteId)
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.entryId").value(100L))
				.andExpect(jsonPath("$.subscribed").value(false));
	}

	@Test
	void subscribe_200_subscribed() throws Exception {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteMapper.findByNoteId(noteId))
				.willReturn(Optional.of(new Note(noteId, 100L, "https://example.com")));
		given(this.noteService.subscribe(noteId, readerId))
				.willReturn(SubscriptionStatus.EXISTING);
		this.mockMvc
				.perform(post("/notes/{noteId}/subscribe", noteId)
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.entryId").value(100L))
				.andExpect(jsonPath("$.subscribed").value(true));
	}

	@Test
	void subscribe_404() throws Exception {
		final NoteId noteId = NoteId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.noteMapper.findByNoteId(noteId)).willReturn(Optional.empty());
		this.mockMvc
				.perform(post("/notes/{noteId}/subscribe", noteId)
						.with(jwt().jwt(jwt -> jwt.subject(readerId.toString())
								.claim("scope", List.of("note:read")).build())))
				.andExpect(status().isNotFound());
	}
}