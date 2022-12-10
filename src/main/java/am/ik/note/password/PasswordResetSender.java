package am.ik.note.password;

public interface PasswordResetSender {
	void sendLink(PasswordReset passwordReset);

	void notifyReset(PasswordReset passwordReset);
}