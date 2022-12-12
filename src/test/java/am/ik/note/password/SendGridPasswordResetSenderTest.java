package am.ik.note.password;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import am.ik.note.reader.Reader;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.sendgrid.SendGridSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SendGridPasswordResetSenderTest {

	@Test
	void sendLink() {
		final SendGridSender sendGridSender = mock(SendGridSender.class);
		final ReaderMapper readerMapper = mock(ReaderMapper.class);
		final SendGridPasswordResetSender passwordResetSender = new SendGridPasswordResetSender(
				sendGridSender, readerMapper);
		final OffsetDateTime created = OffsetDateTime
				.parse("2022-12-09T10:58:12.445+09:00").toInstant()
				.atOffset(ZoneOffset.UTC);
		final PasswordResetId passwordResetId = PasswordResetId.random();
		final ReaderId readerId = ReaderId.random();
		final PasswordReset passwordReset = new PasswordReset(passwordResetId, readerId,
				created);
		given(readerMapper.findById(readerId)).willReturn(
				Optional.of(new Reader(readerId, "demo@example.com", null, null)));
		passwordResetSender.sendLink(passwordReset);
		final ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
		verify(sendGridSender).sendMail(to.capture(), subject.capture(),
				content.capture());
		assertThat(to.getValue()).isEqualTo("demo@example.com");
		assertThat(subject.getValue()).isEqualTo("【はじめるSpring Boot 3】パスワードリセットリンク通知");
		assertThat(content.getValue()).isEqualTo("""
				こんにちは@makingです。

				次のURLをクリックしてパスワードリセットを行って下さい。

				https://ik.am/note/password_reset/%s

				リンクは2022-12-12T01:58:12.445Zまで有効です。

				お手数おかけしますが、よろしくお願いします。
				""".trim().formatted(passwordResetId));
	}

	@Test
	void notifyReset() {
		final SendGridSender sendGridSender = mock(SendGridSender.class);
		final ReaderMapper readerMapper = mock(ReaderMapper.class);
		final SendGridPasswordResetSender passwordResetSender = new SendGridPasswordResetSender(
				sendGridSender, readerMapper);
		final OffsetDateTime created = OffsetDateTime
				.parse("2022-12-09T10:58:12.445+09:00").toInstant()
				.atOffset(ZoneOffset.UTC);
		final PasswordResetId passwordResetId = PasswordResetId.random();
		final ReaderId readerId = ReaderId.random();
		final PasswordReset passwordReset = new PasswordReset(passwordResetId, readerId,
				created);
		given(readerMapper.findById(readerId)).willReturn(
				Optional.of(new Reader(readerId, "demo@example.com", null, null)));
		passwordResetSender.notifyReset(passwordReset);
		final ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
		verify(sendGridSender).sendMail(to.capture(), subject.capture(),
				content.capture());
		assertThat(to.getValue()).isEqualTo("demo@example.com");
		assertThat(subject.getValue()).isEqualTo("【はじめるSpring Boot 3】パスワードリセット完了通知");
		assertThat(content.getValue()).isEqualTo("""
				こんにちは@makingです。

				パスワードがリセットされました。次のURLからログインして下さい。

				https://ik.am/note/login
				""".trim().formatted(passwordResetId));
	}
}