package am.ik.note.content;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.springframework.lang.NonNull;

public record NoteSummary(@JsonUnwrapped NoteId noteId, @NonNull Long entryId, @NonNull String title,
						  @NonNull String noteUrl,
						  @NonNull boolean subscribed,
						  @NonNull OffsetDateTime updatedDate) {

	public NoteSummary excludeNoteId() {
		return new NoteSummary(null, this.entryId, this.title, this.noteUrl, this.subscribed, this.updatedDate);
	}
}
