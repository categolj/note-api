package am.ik.note.content;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record NoteSummary(@JsonUnwrapped NoteId noteId, Long entryId, String title,
						  String noteUrl,
						  boolean subscribed) {

	public NoteSummary excludeNoteId() {
		return new NoteSummary(null, this.entryId, this.title, this.noteUrl, this.subscribed);
	}
}
