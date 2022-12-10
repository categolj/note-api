package am.ik.note.content;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record Note(@JsonUnwrapped NoteId noteId, Long entryId, String noteUrl) {
}
