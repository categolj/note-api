package am.ik.note.password;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PasswordResetId(@JsonProperty("passwordResetId") UUID value) {

	public static PasswordResetId random() {
		return new PasswordResetId(UUID.randomUUID());
	}

	public static PasswordResetId valueOf(String value) {
		return new PasswordResetId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return Objects.toString(this.value);
	}
}
