package am.ik.note.reader.activationlink;

public interface ActivationLinkSender {
	void sendActivationLink(String email, ActivationLink activationLink);
}