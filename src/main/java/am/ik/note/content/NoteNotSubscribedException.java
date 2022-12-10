package am.ik.note.content;

public class NoteNotSubscribedException extends RuntimeException {
	private String noteUrl;

	public NoteNotSubscribedException(String message, String noteUrl) {
		super(message);
		this.noteUrl = noteUrl;
	}

	public String getNoteUrl() {
		return noteUrl;
	}
}