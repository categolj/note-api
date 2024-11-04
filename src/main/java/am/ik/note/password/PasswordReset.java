package am.ik.note.password;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import am.ik.note.reader.ReaderId;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record PasswordReset(@JsonUnwrapped PasswordResetId resetId, ReaderId readerId,
							OffsetDateTime createdAt) {
	public boolean isValid(Clock clock) {
		final OffsetDateTime now = OffsetDateTime.now(clock);
		return now.isBefore(this.expiry());
	}

	public OffsetDateTime expiry() {
		return this.createdAt.plusDays(3);
	}
}
