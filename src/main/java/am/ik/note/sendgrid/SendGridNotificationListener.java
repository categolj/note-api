package am.ik.note.sendgrid;

import am.ik.note.password.PasswordResetCompletedEvent;
import am.ik.note.password.PasswordResetLinkSendEvent;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.ActivationLinkSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class SendGridNotificationListener {
	private final ReaderMapper readerMapper;

	private final SendGridSender sendGridSender;

	private final Logger logger = LoggerFactory
			.getLogger(SendGridNotificationListener.class);

	public SendGridNotificationListener(ReaderMapper readerMapper,
			SendGridSender sendGridSender) {
		this.readerMapper = readerMapper;
		this.sendGridSender = sendGridSender;
	}

	@ApplicationModuleListener
	void onActivationLinkSend(ActivationLinkSendEvent event) {
		logger.info("Received activation link send event: {}", event);
		final String subject = "【はじめるSpring Boot 3】アカウントアクティベーションリンク通知";
		final String content = """
				こんにちは@makingです。

				購読ありがとうございます。次のURLをクリックしてアカウントのアクティベートを行って下さい。

				%s

				リンクは%sまで有効です。""".formatted(event.link(), event.expiry());
		this.sendGridSender.sendMail(event.email(), subject, content);
	}

	@ApplicationModuleListener
	void onPasswordResetLinkSend(PasswordResetLinkSendEvent event) {
		logger.info("Received password reset link send event: {}", event);
		final String subject = "【はじめるSpring Boot 3】パスワードリセットリンク通知";
		final String content = """
				こんにちは@makingです。

				次のURLをクリックしてパスワードリセットを行って下さい。

				%s

				リンクは%sまで有効です。

				お手数おかけしますが、よろしくお願いします。""".formatted(event.link(), event.expiry());
		this.readerMapper.findById(event.readerId()).ifPresent(
				reader -> this.sendGridSender.sendMail(reader.email(), subject, content));
	}

	@ApplicationModuleListener
	void onPasswordResetCompleted(PasswordResetCompletedEvent event) {
		logger.info("Received password reset completed event: {}", event);
		final String subject = "【はじめるSpring Boot 3】パスワードリセット完了通知";
		final String content = """
				こんにちは@makingです。

				パスワードがリセットされました。次のURLからログインして下さい。

				https://ik.am/note/login""";
		this.readerMapper.findById(event.readerId()).ifPresent(
				reader -> this.sendGridSender.sendMail(reader.email(), subject, content));
	}
}
