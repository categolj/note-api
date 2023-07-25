package am.ik.note.password;

import am.ik.note.reader.ReaderMapper;
import am.ik.note.sendgrid.SendGridSender;

import org.springframework.stereotype.Component;

@Component
public class SendGridPasswordResetSender implements PasswordResetSender {
	private final SendGridSender sendGridSender;

	private final ReaderMapper readerMapper;

	public SendGridPasswordResetSender(SendGridSender sendGridSender,
			ReaderMapper readerMapper) {
		this.sendGridSender = sendGridSender;
		this.readerMapper = readerMapper;
	}

	@Override
	public void sendLink(PasswordReset passwordReset) {
		this.readerMapper.findById(passwordReset.readerId()).ifPresent(reader -> {
			final String to = reader.email();
			final String subject = "【はじめるSpring Boot 3】パスワードリセットリンク通知";
			final String link = String.format("https://ik.am/note/password_reset/%s",
					passwordReset.resetId());
			final String content = "こんにちは@makingです。\n\n" + //
					"次のURLをクリックしてパスワードリセットを行って下さい。\n\n" + //
					link + "\n\n" + //
					"リンクは" + passwordReset.expiry() + "まで有効です。\n\n" + //
					"お手数おかけしますが、よろしくお願いします。";
			this.sendGridSender.sendMail(to, subject, content);
		});
	}

	@Override
	public void notifyReset(PasswordReset passwordReset) {
		this.readerMapper.findById(passwordReset.readerId()).ifPresent(reader -> {
			final String to = reader.email();
			final String subject = "【はじめるSpring Boot 3】パスワードリセット完了通知";
			final String content = "こんにちは@makingです。\n\n" + //
					"パスワードがリセットされました。次のURLからログインして下さい。\n\n" + //
					"https://ik.am/note/login";
			this.sendGridSender.sendMail(to, subject, content);
		});
	}
}