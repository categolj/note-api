package am.ik.note.password;

import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderInitializeEvent;
import am.ik.note.reader.ReaderPassword;
import am.ik.note.reader.ReaderPasswordMapper;
import java.net.URI;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import static am.ik.note.password.PasswordResetCompletedEventBuilder.passwordResetCompletedEvent;
import static am.ik.note.password.PasswordResetLinkSendEventBuilder.passwordResetLinkSendEvent;

@Service
public class PasswordResetService {

	private final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	private final PasswordResetMapper passwordResetMapper;

	private final ReaderPasswordMapper readerPasswordMapper;

	private final ApplicationEventPublisher eventPublisher;

	private final PasswordEncoder passwordEncoder;

	private final Clock clock;

	public PasswordResetService(PasswordResetMapper passwordResetMapper, ReaderPasswordMapper readerPasswordMapper,
			ApplicationEventPublisher eventPublisher, PasswordEncoder passwordEncoder, Clock clock) {
		this.passwordResetMapper = passwordResetMapper;
		this.readerPasswordMapper = readerPasswordMapper;
		this.eventPublisher = eventPublisher;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional
	public int sendLink(PasswordReset passwordReset) {
		final int count = this.passwordResetMapper.insert(passwordReset.resetId(), passwordReset.readerId());
		log.info("Send Link: {}", passwordReset);
		URI link = URI.create(String.format("https://ik.am/note/password_reset/%s", passwordReset.resetId()));
		PasswordResetLinkSendEvent event = passwordResetLinkSendEvent().readerId(passwordReset.readerId())
			.link(link)
			.expiry(passwordReset.expiry())
			.build();
		this.eventPublisher.publishEvent(event);
		return count;
	}

	@Transactional
	public int reset(PasswordReset passwordReset, String newPassword) {
		Assert.hasText(newPassword, "newPassword should not be empty");
		if (!passwordReset.isValid(this.clock)) {
			throw new PasswordResetExpiredException();
		}
		final ReaderId readerId = passwordReset.readerId();
		this.readerPasswordMapper.deleteByReaderId(readerId);
		this.passwordResetMapper.deleteByResetId(passwordReset.resetId());
		final String encodedPassword = this.passwordEncoder.encode(newPassword);
		Assert.notNull(encodedPassword, "newPassword should not be null");
		final int count = this.readerPasswordMapper.insert(new ReaderPassword(readerId, encodedPassword));
		this.eventPublisher.publishEvent(new ReaderInitializeEvent(readerId));
		PasswordResetCompletedEvent event = passwordResetCompletedEvent().readerId(passwordReset.readerId()).build();
		this.eventPublisher.publishEvent(event);
		return count;
	}

}