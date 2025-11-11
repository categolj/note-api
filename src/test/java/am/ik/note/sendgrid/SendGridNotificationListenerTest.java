package am.ik.note.sendgrid;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import am.ik.note.TestContainersConfig;
import am.ik.note.password.PasswordResetCompletedEvent;
import am.ik.note.password.PasswordResetLinkSendEvent;
import am.ik.note.reader.ActivationLinkSendEvent;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.ReaderState;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGridAPI;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import static am.ik.note.password.PasswordResetCompletedEventBuilder.passwordResetCompletedEvent;
import static am.ik.note.password.PasswordResetLinkSendEventBuilder.passwordResetLinkSendEvent;
import static am.ik.note.reader.ActivationLinkSendEventBuilder.activationLinkSendEvent;
import static am.ik.note.reader.ReaderBuilder.reader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ApplicationModuleTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig.class)
class SendGridNotificationListenerTest {

	@MockBean
	SendGridAPI sendGrid;

	@MockBean
	ReaderMapper readerMapper;

	@Autowired
	EventPublicationRegistry registry;

	@Test
	void activationLinkSend(Scenario scenario) throws Exception {
		given(this.sendGrid.api(any())).willReturn(new Response(202, "OK", Map.of()));
		ActivationLinkSendEvent event = activationLinkSendEvent().email("foo@example.com")
			.link(URI.create("https://example.com/activation"))
			.expiry(OffsetDateTime.now())
			.build();
		scenario.publish(event).andWaitForStateChange(() -> registry.findIncompletePublications(), Collection::isEmpty);
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(this.sendGrid).api(captor.capture());
		Request request = captor.getValue();
		assertThat(request.getBody()).contains("アカウントアクティベーションリンク通知")
			.contains("foo@example.com")
			.contains(event.link().toString())
			.contains(event.expiry().toString());
	}

	@Test
	void passwordResetLinkSend(Scenario scenario) throws Exception {
		ReaderId readerId = ReaderId.valueOf("44698583-f657-4fda-89b5-6c2f3522e855");
		given(this.readerMapper.findById(readerId)).willReturn(Optional.of(reader().readerId(readerId)
			.email("foo@example.com")
			.hashedPassword("")
			.readerState(ReaderState.ENABLED)
			.build()));
		given(this.sendGrid.api(any())).willReturn(new Response(202, "OK", Map.of()));
		PasswordResetLinkSendEvent event = passwordResetLinkSendEvent().readerId(readerId)
			.link(URI.create("https://example.com/password_reset"))
			.expiry(OffsetDateTime.now())
			.build();
		scenario.publish(event).andWaitForStateChange(() -> registry.findIncompletePublications(), Collection::isEmpty);
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(this.sendGrid).api(captor.capture());
		Request request = captor.getValue();
		assertThat(request.getBody()).contains("パスワードリセットリンク通知")
			.contains("foo@example.com")
			.contains(event.link().toString())
			.contains(event.expiry().toString());
	}

	@Test
	void passwordResetCompleted(Scenario scenario) throws Exception {
		ReaderId readerId = ReaderId.valueOf("44698583-f657-4fda-89b5-6c2f3522e855");
		given(this.readerMapper.findById(readerId)).willReturn(Optional.of(reader().readerId(readerId)
			.email("foo@example.com")
			.hashedPassword("")
			.readerState(ReaderState.ENABLED)
			.build()));
		given(this.sendGrid.api(any())).willReturn(new Response(202, "OK", Map.of()));
		PasswordResetCompletedEvent event = passwordResetCompletedEvent().readerId(readerId).build();
		scenario.publish(event).andWaitForStateChange(() -> registry.findIncompletePublications(), Collection::isEmpty);
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(this.sendGrid).api(captor.capture());
		Request request = captor.getValue();
		assertThat(request.getBody()).contains("パスワードリセット完了通知").contains("foo@example.com");
	}

}