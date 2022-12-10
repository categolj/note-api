package am.ik.note.reader.activationlink;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActivationLinkId(@JsonProperty("activationLinkId") UUID value) {
	public static ActivationLinkId random() {
		return new ActivationLinkId(UUID.randomUUID());
	}

	public static ActivationLinkId valueOf(String value) {
		return new ActivationLinkId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return Objects.toString(this.value);
	}
}
