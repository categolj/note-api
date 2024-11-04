package am.ik.note.reader.activationlink;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import am.ik.note.reader.ReaderId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record ActivationLink(@JsonUnwrapped ActivationLinkId activationId,
							 ReaderId readerId,
							 OffsetDateTime createdAt) {

	public OffsetDateTime expiry() {
		return this.createdAt.plusDays(3);
	}

	@JsonIgnore
	public boolean isValid(Clock clock) {
		OffsetDateTime now = OffsetDateTime.now(clock);
		return now.isBefore(this.expiry());
	}

}
