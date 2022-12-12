package am.ik.note.password;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import am.ik.note.MockConfig;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderInitializer;
import am.ik.note.reader.ReaderPassword;
import am.ik.note.reader.ReaderPasswordMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith({ SpringExtension.class, OutputCaptureExtension.class })
@Import({ PasswordResetService.class, MockConfig.class })
class PasswordResetServiceTest {
	@MockBean
	PasswordResetMapper passwordResetMapper;

	@MockBean
	ReaderPasswordMapper readerPasswordMapper;

	@MockBean
	PasswordResetSender passwordResetSender;

	@MockBean
	ReaderInitializer readerInitializer;

	@Autowired
	PasswordResetService passwordResetService;

	@Test
	void sendLink(CapturedOutput output) {
		final PasswordResetId resetId = PasswordResetId.random();
		given(this.passwordResetMapper.insert(any(), any())).willReturn(1);
		final PasswordReset passwordReset = new PasswordReset(resetId,
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"),
				OffsetDateTime.now());
		final int count = this.passwordResetService.sendLink(passwordReset);
		assertThat(count).isEqualTo(1);
		assertThat(output.toString()).contains("Send Link: " + passwordReset);
	}

	@Test
	void reset() {
		final PasswordResetId resetId = PasswordResetId.random();
		given(this.readerPasswordMapper.insert(any())).willReturn(1);
		final ArgumentCaptor<ReaderPassword> captor = ArgumentCaptor
				.forClass(ReaderPassword.class);
		final PasswordReset passwordReset = new PasswordReset(resetId,
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"),
				OffsetDateTime.parse("2022-12-06T06:38:31.343307Z")
						.minus(3L, ChronoUnit.DAYS).plus(1L, ChronoUnit.MINUTES));
		final int count = this.passwordResetService.reset(passwordReset, "newPassword");
		assertThat(count).isEqualTo(1);
		verify(this.readerPasswordMapper).insert(captor.capture());
		final ReaderPassword readerPassword = captor.getValue();
		assertThat(readerPassword.readerId())
				.isEqualTo(ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"));
		assertThat(readerPassword.hashedPassword()).isEqualTo("{noop}newPassword");
	}

	@Test
	void resetExpired() {
		final PasswordResetId resetId = PasswordResetId.random();
		given(this.readerPasswordMapper.insert(any())).willReturn(1);
		final PasswordReset passwordReset = new PasswordReset(resetId,
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"), OffsetDateTime
						.parse("2022-12-06T06:38:31.343307Z").minus(3L, ChronoUnit.DAYS));
		assertThatThrownBy(
				() -> this.passwordResetService.reset(passwordReset, "newPassword"))
						.isInstanceOf(PasswordResetExpiredException.class);
	}
}