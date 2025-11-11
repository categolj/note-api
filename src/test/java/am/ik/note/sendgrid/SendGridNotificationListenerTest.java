package am.ik.note.sendgrid;

import am.ik.note.TestContainersConfig;
import am.ik.note.config.SendGridConfig;
import am.ik.note.password.PasswordResetCompletedEvent;
import am.ik.note.password.PasswordResetLinkSendEvent;
import am.ik.note.reader.ActivationLinkSendEvent;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.ReaderState;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static am.ik.note.password.PasswordResetCompletedEventBuilder.passwordResetCompletedEvent;
import static am.ik.note.password.PasswordResetLinkSendEventBuilder.passwordResetLinkSendEvent;
import static am.ik.note.reader.ActivationLinkSendEventBuilder.activationLinkSendEvent;
import static am.ik.note.reader.ReaderBuilder.reader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ApplicationModuleTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({ TestContainersConfig.class, SendGridConfig.class })
class SendGridNotificationListenerTest {

	@MockitoBean
	ReaderMapper readerMapper;

	@Autowired
	EventPublicationRegistry registry;

	RestClient restClient;

	int maildevPort;

	@BeforeEach
	void setUp(@Autowired RestClient.Builder restClientBuilder, @Value("${maildev.port}") int maildevPort) {
		this.restClient = restClientBuilder.requestFactory(new SimpleClientHttpRequestFactory())
			.defaultStatusHandler(__ -> true, (req, res) -> {
			})
			.build();
		this.maildevPort = maildevPort;
		this.restClient.delete()
			.uri("http://localhost:{port}/email/all", this.maildevPort)
			.retrieve()
			.toBodilessEntity();
	}

	@Test
	void activationLinkSend(Scenario scenario) throws Exception {
		ActivationLinkSendEvent event = activationLinkSendEvent().email("foo@example.com")
			.link(URI.create("https://example.com/activation"))
			.expiry(OffsetDateTime.now())
			.build();
		scenario.publish(event).andWaitForStateChange(() -> registry.findIncompletePublications(), Collection::isEmpty);
		AtomicReference<List<Map<String, Object>>> received = new AtomicReference<>();
		Awaitility.await().until(() -> {
			received.set(this.restClient.get()
				.uri("http://localhost:{port}/email", this.maildevPort)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				}));
			return received.get() != null && received.get().size() == 1;
		});
		assertThat(received.get()).hasSize(1);
		assertThat(received.get().get(0)).containsEntry("subject", "【はじめるSpring Boot 3】アカウントアクティベーションリンク通知");
		assertThat(received.get().get(0)).containsKey("text");
		assertThat(received.get().get(0).get("text").toString())
			.contains("購読ありがとうございます。次のURLをクリックしてアカウントのアクティベートを行って下さい。");
		assertThat(received.get().get(0)).containsEntry("to",
				List.of(Map.of("address", "foo@example.com", "name", "")));
		assertThat(received.get().get(0)).containsEntry("from",
				List.of(Map.of("address", "noreply@ik.am", "name", "")));
	}

	@Test
	void passwordResetLinkSend(Scenario scenario) throws Exception {
		ReaderId readerId = ReaderId.valueOf("44698583-f657-4fda-89b5-6c2f3522e855");
		given(this.readerMapper.findById(readerId)).willReturn(Optional.of(reader().readerId(readerId)
			.email("foo@example.com")
			.hashedPassword("")
			.readerState(ReaderState.ENABLED)
			.build()));
		PasswordResetLinkSendEvent event = passwordResetLinkSendEvent().readerId(readerId)
			.link(URI.create("https://example.com/password_reset"))
			.expiry(OffsetDateTime.now())
			.build();
		scenario.publish(event).andWaitForStateChange(() -> registry.findIncompletePublications(), Collection::isEmpty);
		{
			AtomicReference<List<Map<String, Object>>> received = new AtomicReference<>();
			Awaitility.await().until(() -> {
				received.set(this.restClient.get()
					.uri("http://localhost:{port}/email", this.maildevPort)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					}));
				return received.get() != null && received.get().size() == 1;
			});
			assertThat(received.get()).hasSize(1);
			assertThat(received.get().get(0)).containsEntry("subject", "【はじめるSpring Boot 3】パスワードリセットリンク通知");
			assertThat(received.get().get(0)).containsKey("text");
			assertThat(received.get().get(0).get("text").toString()).contains("次のURLをクリックしてパスワードリセットを行って下さい。");
			assertThat(received.get().get(0)).containsEntry("to",
					List.of(Map.of("address", "foo@example.com", "name", "")));
			assertThat(received.get().get(0)).containsEntry("from",
					List.of(Map.of("address", "noreply@ik.am", "name", "")));
		}
	}

	@Test
	void passwordResetCompleted(Scenario scenario) throws Exception {
		ReaderId readerId = ReaderId.valueOf("44698583-f657-4fda-89b5-6c2f3522e855");
		given(this.readerMapper.findById(readerId)).willReturn(Optional.of(reader().readerId(readerId)
			.email("foo@example.com")
			.hashedPassword("")
			.readerState(ReaderState.ENABLED)
			.build()));
		PasswordResetCompletedEvent event = passwordResetCompletedEvent().readerId(readerId).build();
		scenario.publish(event).andWaitForStateChange(() -> registry.findIncompletePublications(), Collection::isEmpty);
		{
			AtomicReference<List<Map<String, Object>>> received = new AtomicReference<>();
			Awaitility.await().until(() -> {
				received.set(this.restClient.get()
					.uri("http://localhost:{port}/email", this.maildevPort)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					}));
				return received.get() != null && received.get().size() == 1;
			});
			assertThat(received.get()).hasSize(1);
			assertThat(received.get().get(0)).containsEntry("subject", "【はじめるSpring Boot 3】パスワードリセット完了通知");
			assertThat(received.get().get(0)).containsKey("text");
			assertThat(received.get().get(0).get("text").toString()).contains("パスワードがリセットされました。次のURLからログインして下さい。");
			assertThat(received.get().get(0)).containsEntry("to",
					List.of(Map.of("address", "foo@example.com", "name", "")));
			assertThat(received.get().get(0)).containsEntry("from",
					List.of(Map.of("address", "noreply@ik.am", "name", "")));
		}
	}

}