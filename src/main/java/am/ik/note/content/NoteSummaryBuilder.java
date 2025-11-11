package am.ik.note.content;

import java.time.OffsetDateTime;

public class NoteSummaryBuilder {

	private NoteId noteId;

	private Long entryId;

	private String title;

	private String noteUrl;

	private boolean subscribed;

	private OffsetDateTime updatedDate;

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

	public Long getEntryId() {
		return entryId;
	}

	public NoteSummary build() {
		return new NoteSummary(this.noteId, this.entryId, this.title, this.noteUrl, this.subscribed, this.updatedDate);
	}

}