package am.ik.note.password;

import java.time.Clock;

import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderInitializer;
import am.ik.note.reader.ReaderPassword;
import am.ik.note.reader.ReaderPasswordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
	private final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	private final PasswordResetMapper passwordResetMapper;

	private final ReaderPasswordMapper readerPasswordMapper;

	private final PasswordResetSender passwordResetSender;

	private final PasswordEncoder passwordEncoder;

	private final ReaderInitializer readerInitializer;

	private final Clock clock;

	public PasswordResetService(PasswordResetMapper passwordResetMapper, ReaderPasswordMapper readerPasswordMapper, PasswordResetSender passwordResetSender, PasswordEncoder passwordEncoder, ReaderInitializer readerInitializer, Clock clock) {
		this.passwordResetMapper = passwordResetMapper;
		this.readerPasswordMapper = readerPasswordMapper;
		this.passwordResetSender = passwordResetSender;
		this.passwordEncoder = passwordEncoder;
		this.readerInitializer = readerInitializer;
		this.clock = clock;
	}

	@Transactional
	public int sendLink(PasswordReset passwordReset) {
		final int count = this.passwordResetMapper.insert(passwordReset.resetId(), passwordReset.readerId());
		log.info("Send Link: {}", passwordReset);
		this.passwordResetSender.sendLink(passwordReset);
		return count;
	}

	@Transactional
	public int reset(PasswordReset passwordReset, String newPassword) {
		if (!passwordReset.isValid(this.clock)) {
			throw new PasswordResetExpiredException();
		}
		final ReaderId readerId = passwordReset.readerId();
		this.readerPasswordMapper.deleteByReaderId(readerId);
		this.passwordResetMapper.deleteByResetId(passwordReset.resetId());
		final String encodedPassword = this.passwordEncoder.encode(newPassword);
		final int count = this.readerPasswordMapper.insert(new ReaderPassword(readerId, encodedPassword));
		this.readerInitializer.initialize(readerId);
		this.passwordResetSender.notifyReset(passwordReset);
		return count;
	}
}