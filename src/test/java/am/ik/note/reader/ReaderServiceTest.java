package am.ik.note.reader;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import am.ik.note.MockConfig;
import am.ik.note.MockIdGenerator;
import am.ik.note.TestContainersConfig;
import am.ik.note.reader.activationlink.ActivationLink;
import am.ik.note.reader.activationlink.ActivationLinkExpiredException;
import am.ik.note.reader.activationlink.ActivationLinkId;
import am.ik.note.reader.activationlink.ActivationLinkMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Import({ MockConfig.class, TestContainersConfig.class })
@ApplicationModuleTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ReaderServiceTest {
	@MockBean
	ReaderMapper readerMapper;

	@MockBean
	ReaderPasswordMapper readerPasswordMapper;

	@MockBean
	ActivationLinkMapper activationLinkMapper;

	@Autowired
	MockIdGenerator idGenerator;

	@Autowired
	ReaderService readerService;

	@Test
	void createReader(AssertablePublishedEvents events) {
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
		verify(this.readerPasswordMapper).insert(captor.capture());
		final ReaderPassword readerPassword = captor.getValue();
		assertThat(readerPassword.readerId())
				.isEqualTo(ReaderId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"));
		assertThat(readerPassword.hashedPassword()).isEqualTo("{noop}password");
		assertThat(events).contains(ActivationLinkSendEvent.class)
				.matching(ActivationLinkSendEvent::email, "demo@example.com")
				.matching(ActivationLinkSendEvent::link, URI.create(
						"https://ik.am/note/readers/b734fc36-9985-45c0-adf2-f799e8d641e9/activations/931f3d49-4f48-4214-bab3-c5b659b6b24c"))
				.matching(ActivationLinkSendEvent::expiry,
						OffsetDateTime.parse("2022-12-09T06:38:31.343307Z"));
	}

	@Test
	void createReaderExisting(AssertablePublishedEvents events) {
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
		verify(this.readerPasswordMapper).insert(captor.capture());
		final ReaderPassword readerPassword = captor.getValue();
		assertThat(readerPassword.readerId())
				.isEqualTo(ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"));
		assertThat(readerPassword.hashedPassword()).isEqualTo("{noop}password");
		assertThat(events).contains(ActivationLinkSendEvent.class)
				.matching(ActivationLinkSendEvent::email, "demo@example.com")
				.matching(ActivationLinkSendEvent::link, URI.create(
						"https://ik.am/note/readers/c872edeb-1d86-4c1a-81ac-895ace606ec4/activations/b734fc36-9985-45c0-adf2-f799e8d641e9"))
				.matching(ActivationLinkSendEvent::expiry,
						OffsetDateTime.parse("2022-12-09T06:38:31.343307Z"));
	}

	@Test
	void activate(AssertablePublishedEvents events) {
		final ActivationLink activationLink = new ActivationLink(
				ActivationLinkId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"),
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"),
				OffsetDateTime.parse("2022-12-06T06:38:31.343307Z").minusDays(3L)
						.plusMinutes(1L));
		final ActivationLinkId activationLinkId = this.readerService
				.activate(activationLink);
		assertThat(activationLinkId).isEqualTo(
				ActivationLinkId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"));
		assertThat(events).contains(ReaderInitializeEvent.class).matching(
				ReaderInitializeEvent::readerId,
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"));
	}

	@Test
	void activateExpired() {
		final ActivationLink activationLink = new ActivationLink(
				ActivationLinkId.valueOf("b734fc36-9985-45c0-adf2-f799e8d641e9"),
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"),
				OffsetDateTime.parse("2022-12-06T06:38:31.343307Z").minusDays(3L));
		assertThatThrownBy(() -> this.readerService.activate(activationLink))
				.isInstanceOf(ActivationLinkExpiredException.class);
	}

}