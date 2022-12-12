package am.ik.note.reader.activationlink;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import am.ik.note.reader.ReaderId;
import am.ik.note.sendgrid.SendGridSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SendGridActiveLinkSenderTest {

	@Test
	void sendActivationLink() {
		final SendGridSender sendGridSender = mock(SendGridSender.class);
		final SendGridActiveLinkSender sendGridActiveLinkSender = new SendGridActiveLinkSender(
				sendGridSender);
		final ReaderId readerId = ReaderId.random();
		final ActivationLinkId activationLinkId = ActivationLinkId.random();
		final OffsetDateTime created = OffsetDateTime
				.parse("2022-12-09T10:58:12.445+09:00").toInstant()
				.atOffset(ZoneOffset.UTC);
		final ActivationLink activationLink = new ActivationLink(activationLinkId,
				readerId, created);
		sendGridActiveLinkSender.sendActivationLink("demo@example.com", activationLink);
		final ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
		verify(sendGridSender).sendMail(to.capture(), subject.capture(),
				content.capture());
		assertThat(to.getValue()).isEqualTo("demo@example.com");
		assertThat(subject.getValue())
				.isEqualTo("【はじめるSpring Boot 3】アカウントアクティベーションリンク通知");
		assertThat(content.getValue()).isEqualTo("""
				こんにちは@makingです。

				購読ありがとうございます。次のURLをクリックしてアカウントのアクティベートを行って下さい。

				https://ik.am/note/readers/%s/activations/%s

				リンクは2022-12-12T01:58:12.445Zまで有効です。
				""".trim().formatted(readerId, activationLinkId));

	}
}