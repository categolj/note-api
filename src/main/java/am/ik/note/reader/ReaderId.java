package am.ik.note.reader;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.NonNull;

public record ReaderId(@NonNull @JsonProperty("readerId") UUID value) {

	public static ReaderId random() {
		return new ReaderId(UUID.randomUUID());
	}

	public static ReaderId valueOf(String value) {
		return new ReaderId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return Objects.toString(this.value);
	}
}
