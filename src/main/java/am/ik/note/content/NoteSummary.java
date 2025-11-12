package am.ik.note.content;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

public record NoteSummary(@Nullable @JsonUnwrapped NoteId noteId, Long entryId, @Nullable String title, String noteUrl,
		boolean subscribed, @Nullable OffsetDateTime updatedDate) {

	public NoteSummary excludeNoteId() {
		return new NoteSummary(null, this.entryId, this.title, this.noteUrl, this.subscribed, this.updatedDate);
	}
}
