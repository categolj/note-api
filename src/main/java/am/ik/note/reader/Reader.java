package am.ik.note.reader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record Reader(@JsonUnwrapped ReaderId readerId, String email,
					 String hashedPassword,
					 ReaderState readerState) {

	@Override
	@JsonIgnore
	public String hashedPassword() {
		return hashedPassword;
	}

	public boolean isLocked() {
		return ReaderState.LOCKED == this.readerState;
	}

	public boolean isDisabled() {
		return ReaderState.DISABLED == this.readerState;
	}
}
