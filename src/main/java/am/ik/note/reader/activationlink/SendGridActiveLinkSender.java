package am.ik.note.reader.activationlink;

import am.ik.note.sendgrid.SendGridSender;

import org.springframework.stereotype.Component;

@Component
public class SendGridActiveLinkSender implements ActivationLinkSender {
	private final SendGridSender sendGridSender;

	public SendGridActiveLinkSender(SendGridSender sendGridSender) {
		this.sendGridSender = sendGridSender;
	}

	@Override
	public void sendActivationLink(String email, ActivationLink activationLink) {
		final String subject = "【はじめるSpring Boot 3】アカウントアクティベーションリンク通知";
		final String link = String.format("https://ik.am/note/readers/%s/activations/%s", activationLink.readerId(), activationLink.activationId());
		final String content = "こんにちは@makingです。\n\n" + //
							   "購読ありがとうございます。次のURLをクリックしてアカウントのアクティベートを行って下さい。\n\n" +  //
							   link + "\n\n" + //
							   "リンクは" + activationLink.expiry() + "まで有効です。";
		this.sendGridSender.sendMail(email, subject, content);
	}
}