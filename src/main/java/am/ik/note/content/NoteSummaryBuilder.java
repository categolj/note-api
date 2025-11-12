package am.ik.note.content;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

public class NoteSummaryBuilder {

	@Nullable private NoteId noteId;

	@Nullable private Long entryId;

	@Nullable private String title;

	@Nullable private String noteUrl;

	private boolean subscribed;

	@Nullable private OffsetDateTime updatedDate;

	public NoteSummaryBuilder withNoteId(NoteId noteId) {
		this.noteId = noteId;
		return this;
	}

	public NoteSummaryBuilder withEntryId(Long entryId) {
		this.entryId = entryId;
		return this;
	}

	public NoteSummaryBuilder withTitle(String title) {
		this.title = title;
		return this;
	}

	public NoteSummaryBuilder withNoteUrl(String noteUrl) {
		this.noteUrl = noteUrl;
		return this;
	}

	public NoteSummaryBuilder withSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
		return this;
	}

	public NoteSummaryBuilder withUpdatedDate(OffsetDateTime updatedDate) {
		this.updatedDate = updatedDate;
		return this;
	}

	@Nullable public Long getEntryId() {
		return entryId;
	}

	public NoteSummary build() {
		Assert.notNull(this.entryId, "entryId is required");
		Assert.notNull(this.noteUrl, "noteUrl is required");
		return new NoteSummary(this.noteId, this.entryId, this.title, this.noteUrl, this.subscribed, this.updatedDate);
	}

}