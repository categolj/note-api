package am.ik.note.content.web;

import java.util.Map;
import java.util.UUID;

import am.ik.note.content.NoteDetails;
import am.ik.note.content.NoteId;
import am.ik.note.content.NoteMapper;
import am.ik.note.content.NoteNotSubscribedException;
import am.ik.note.content.NoteService;
import am.ik.note.content.NoteService.SubscriptionStatus;
import am.ik.note.content.NoteSummary;
import am.ik.note.reader.ReaderId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/notes")
public class NoteController {
	private final NoteService noteService;

	private final NoteMapper noteMapper;

	public NoteController(NoteService noteService, NoteMapper noteMapper) {
		this.noteService = noteService;
		this.noteMapper = noteMapper;
	}

	@GetMapping(path = "")
	public ResponseEntity<?> getNotes(@AuthenticationPrincipal Jwt jwt) {
		final ReaderId readerId = ReaderId.valueOf(jwt.getSubject());
		return ResponseEntity.ok(this.noteService.findAll(readerId).stream()
				.map(NoteSummary::excludeNoteId).toList());
	}

	@GetMapping(path = "/{entryId:[0-9]+}")
	public ResponseEntity<?> getNoteByEntryId(@PathVariable("entryId") Long entryId,
			@AuthenticationPrincipal Jwt jwt) {
		try {
			final ReaderId readerId = ReaderId.valueOf(jwt.getSubject());
			return ResponseEntity.of(this.noteService.findByEntryId(entryId, readerId)
					.map(NoteDetails::excludeNoteId));
		}
		catch (NoteNotSubscribedException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("message", e.getMessage(), "noteUrl", e.getNoteUrl()));
		}
	}

	@GetMapping(path = "/{noteId:[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}}")
	public ResponseEntity<?> getNoteByNoteId(@PathVariable("noteId") NoteId noteId,
			@AuthenticationPrincipal Jwt jwt) {
		try {
			final ReaderId readerId = ReaderId.valueOf(jwt.getSubject());
			return ResponseEntity.of(this.noteService.findByNoteId(noteId, readerId));
		}
		catch (NoteNotSubscribedException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("message", e.getMessage(), "noteUrl", e.getNoteUrl()));
		}
	}

	@DeleteMapping(path = "")
	public ResponseEntity<?> deleteByEntryId(@RequestParam("entryId") Long entryId) {
		final int count = this.noteMapper.deleteByEntryId(entryId);
		return ResponseEntity.ok(Map.of("message", String.format("deleted (%d)", count)));
	}

	@PutMapping(path = "/{entryId:[0-9]+}")
	public ResponseEntity<?> putNote(@PathVariable("entryId") Long entryId,
			@RequestBody PutNoteInput input) {
		this.noteMapper.insertNote(input.toNoteId(), entryId, input.noteUrl());
		return ResponseEntity.ok().build();
	}

	@PostMapping(path = "/{noteId:[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}}/subscribe")
	public ResponseEntity<?> subscribe(@PathVariable("noteId") NoteId noteId,
			@AuthenticationPrincipal Jwt jwt) {
		return this.noteMapper.findByNoteId(noteId).map(note -> {
			final ReaderId readerId = ReaderId.valueOf(jwt.getSubject());
			final SubscriptionStatus status = this.noteService.subscribe(noteId,
					readerId);
			final Long entryId = note.entryId();
			return ResponseEntity
					.ok(new SubscribeOutput(entryId, status.isAlreadySubscribed()));
		}).orElseGet(() -> ResponseEntity.notFound().build());
	}

	record PutNoteInput(UUID noteId, String noteUrl) {
		public NoteId toNoteId() {
			return new NoteId(this.noteId);
		}
	}

	record SubscribeOutput(Long entryId, boolean subscribed) {

	}
}
