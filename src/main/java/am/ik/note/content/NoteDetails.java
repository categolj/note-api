package am.ik.note.content;

import am.ik.note.entry.Author;
import am.ik.note.entry.FrontMatter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.springframework.lang.NonNull;

@JsonInclude(Include.NON_EMPTY)
public record NoteDetails(@JsonUnwrapped NoteId noteId, @NonNull Long entryId, @NonNull String content,
		@NonNull FrontMatter frontMatter, @NonNull String noteUrl, Author created, @NonNull Author updated) {
	public NoteDetails excludeNoteId() {
		return new NoteDetails(null, this.entryId, content, frontMatter, noteUrl, created, updated);
	}
}
