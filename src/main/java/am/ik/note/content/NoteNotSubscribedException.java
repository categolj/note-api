package am.ik.note.content;

import java.util.Objects;

public class NoteNotSubscribedException extends RuntimeException {

	private String noteUrl;

	public NoteNotSubscribedException(String message, String noteUrl) {
		super(message);
		this.noteUrl = noteUrl;
	}

	public String getNoteUrl() {
		return noteUrl;
	}

	@Override
	public String getMessage() {
		return Objects.requireNonNull(super.getMessage());
	}

}