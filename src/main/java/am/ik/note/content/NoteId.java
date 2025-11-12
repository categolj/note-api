package am.ik.note.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record NoteId(@Nullable @JsonProperty("noteId") UUID value) {
	public static NoteId random() {
		return new NoteId(UUID.randomUUID());
	}

	public static NoteId valueOf(String value) {
		return new NoteId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return Objects.toString(this.value);
	}
}
