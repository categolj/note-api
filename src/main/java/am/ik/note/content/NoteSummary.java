package am.ik.note.content;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record NoteSummary(@JsonUnwrapped NoteId noteId, @NonNull Long entryId, @Nullable String title,
		@NonNull String noteUrl, @NonNull boolean subscribed, @Nullable OffsetDateTime updatedDate) {

	public NoteSummary excludeNoteId() {
		return new NoteSummary(null, this.entryId, this.title, this.noteUrl, this.subscribed, this.updatedDate);
	}
}
