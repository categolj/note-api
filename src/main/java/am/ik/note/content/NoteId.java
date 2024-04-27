package am.ik.note.content;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.NonNull;

public record NoteId(@NonNull @JsonProperty("noteId") UUID value) {
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
