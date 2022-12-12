package am.ik.note.reader;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import am.ik.note.MockConfig;
import am.ik.note.MockIdGenerator;
import am.ik.note.reader.activationlink.ActivationLink;
import am.ik.note.reader.activationlink.ActivationLinkExpiredException;
import am.ik.note.reader.activationlink.ActivationLinkId;
import am.ik.note.reader.activationlink.ActivationLinkMapper;
import am.ik.note.reader.activationlink.ActivationLinkSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith({ SpringExtension.class, OutputCaptureExtension.class })
@Import({ ReaderService.class, MockConfig.class })
class ReaderServiceTest {
	@MockBean
	ReaderMapper readerMapper;

	@MockBean
	ReaderPasswordMapper readerPasswordMapper;

	@MockBean
	ActivationLinkMapper activationLinkMapper;

	@MockBean
	ActivationLinkSender activationLinkSender;

	@MockBean
	ReaderInitializer readerInitializer;

	@Autowired
	MockIdGenerator idGenerator;

	@Autowired
	ReaderService readerService;

	@Test
	void createReader(CapturedOutput capture) {
		this.idGenerator.putId(UUID.fromString("b734fc36-9985-45c0-adf2-f799e8d641e9"));
		this.idGenerator.putId(UUID.fromString("931f3d49-4f48-4214-bab3-c5b659b6b24c"));
		final ArgumentCaptor<ReaderPassword> captor = ArgumentCaptor
				.forClass(ReaderPassword.class);
		given(this.readerMapper.findByEmail("demo@example.com"))
				.willReturn(Optional.empty());
		final ActivationLink activationLink = this.readerService
				.createReader("demo@example.com", "password");
		assertThat(activationLink.readerId())
				.isEqualTo(ReaderId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"));
		assertThat(activationLink.activationId()).isEqualTo(
				ActivationLinkId.valueOf("931f3d49-4f48-4214-bab3-c5b659b6b24c"));
		assertThat(activationLink.createdAt())
				.isEqualTo(OffsetDateTime.parse("2022-12-06T06:38:31.343307Z"));
		assertThat(capture.toString()).contains(
				"sendActivationLink: demo@example.com 931f3d49-4f48-4214-bab3-c5b659b6b24c");
		verify(this.readerPasswordMapper).insert(captor.capture());
		final ReaderPassword readerPassword = captor.getValue();
		assertThat(readerPassword.readerId())
				.isEqualTo(ReaderId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"));
		assertThat(readerPassword.hashedPassword()).isEqualTo("{noop}password");
	}

	@Test
	void createReaderExisting(CapturedOutput capture) {
		this.idGenerator.putId(UUID.fromString("b734fc36-9985-45c0-adf2-f799e8d641e9"));
		final ArgumentCaptor<ReaderPassword> captor = ArgumentCaptor
				.forClass(ReaderPassword.class);
		given(this.readerMapper.findByEmail("demo@example.com")).willReturn(Optional
				.of(new Reader(ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"),
						"demo@example.com", "aa", ReaderState.DISABLED)));
		final ActivationLink activationLink = this.readerService
				.createReader("demo@example.com", "password");
		assertThat(activationLink.readerId())
				.isEqualTo(ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"));
		assertThat(activationLink.activationId()).isEqualTo(
				ActivationLinkId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"));
		assertThat(activationLink.createdAt())
				.isEqualTo(OffsetDateTime.parse("2022-12-06T06:38:31.343307Z"));
		assertThat(capture.toString()).contains(
				"sendActivationLink: demo@example.com b734fc36-9985-45c0-adf2-f799e8d641e9");
		verify(this.readerPasswordMapper).insert(captor.capture());
		final ReaderPassword readerPassword = captor.getValue();
		assertThat(readerPassword.readerId())
				.isEqualTo(ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"));
		assertThat(readerPassword.hashedPassword()).isEqualTo("{noop}password");
	}

	@Test
	void activate() {
		final ActivationLink activationLink = new ActivationLink(
				ActivationLinkId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"),
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"),
				OffsetDateTime.parse("2022-12-06T06:38:31.343307Z")
						.minus(3L, ChronoUnit.DAYS).plus(1L, ChronoUnit.MINUTES));
		final ActivationLinkId activationLinkId = this.readerService
				.activate(activationLink);
		assertThat(activationLinkId).isEqualTo(
				ActivationLinkId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"));
	}

	@Test
	void activateExpired() {
		final ActivationLink activationLink = new ActivationLink(
				ActivationLinkId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"),
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"), OffsetDateTime
						.parse("2022-12-06T06:38:31.343307Z").minus(3L, ChronoUnit.DAYS));
		assertThatThrownBy(() -> {
			this.readerService.activate(activationLink);
		}).isInstanceOf(ActivationLinkExpiredException.class);
	}

}