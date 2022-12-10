package am.ik.note.reader;

import am.ik.note.content.NoteId;
import am.ik.note.content.NoteReaderMapper;
import am.ik.note.content.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReaderInitializer {
	private final Logger log = LoggerFactory.getLogger(ReaderInitializer.class);

	private final NoteService noteService;

	private final NoteReaderMapper noteReaderMapper;

	public ReaderInitializer(NoteService noteService, NoteReaderMapper noteReaderMapper) {
		this.noteService = noteService;
		this.noteReaderMapper = noteReaderMapper;
	}

	@Transactional
	public void initialize(ReaderId readerId) {
		try {
			// entryId = 100
			this.subscribeIfNotSubscribed(NoteId.valueOf("9d341d4d-f7e8-400d-82f1-95e05bd9fc0b"), readerId);
			// entryId = 200
			this.subscribeIfNotSubscribed(NoteId.valueOf("e48ec0f9-18d8-442b-8e2a-d1db38593ceb"), readerId);
		}
		catch (RuntimeException e) {
			// Ignore subscription failure
			log.warn("Failed to subscribe initial notes", e);
		}
	}

	void subscribeIfNotSubscribed(NoteId noteId, ReaderId readerId) {
		final int count = this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId);
		if (count == 0) {
			this.noteService.subscribe(noteId, readerId);
		}
	}
}
