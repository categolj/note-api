package am.ik.note.content;

import am.ik.note.entry.Author;
import am.ik.note.entry.FrontMatter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonInclude(Include.NON_EMPTY)
public record NoteDetails(
		@JsonUnwrapped
		NoteId noteId, Long entryId, String content,
		FrontMatter frontMatter, String noteUrl, Author created,
		Author updated) {
	public NoteDetails excludeNoteId() {
		return new NoteDetails(null, this.entryId, content, frontMatter, noteUrl, created, updated);
	}
}
