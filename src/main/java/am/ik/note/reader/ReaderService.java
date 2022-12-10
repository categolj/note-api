package am.ik.note.reader;

import java.time.Clock;
import java.time.OffsetDateTime;

import am.ik.note.reader.activationlink.ActivationLink;
import am.ik.note.reader.activationlink.ActivationLinkExpiredException;
import am.ik.note.reader.activationlink.ActivationLinkId;
import am.ik.note.reader.activationlink.ActivationLinkMapper;
import am.ik.note.reader.activationlink.ActivationLinkSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

@Service
public class ReaderService {
	private final Logger log = LoggerFactory.getLogger(ReaderService.class);

	private final ReaderMapper readerMapper;

	private final ReaderPasswordMapper readerPasswordMapper;

	private final ActivationLinkMapper activationLinkMapper;

	private final ActivationLinkSender activationLinkSender;

	private final ReaderInitializer readerInitializer;

	private final PasswordEncoder passwordEncoder;

	private final IdGenerator idGenerator;

	private final Clock clock;

	public ReaderService(ReaderMapper readerMapper, ReaderPasswordMapper readerPasswordMapper, ActivationLinkMapper activationLinkMapper, ActivationLinkSender activationLinkSender, ReaderInitializer readerInitializer, PasswordEncoder passwordEncoder, IdGenerator idGenerator, Clock clock) {
		this.readerMapper = readerMapper;
		this.readerPasswordMapper = readerPasswordMapper;
		this.activationLinkMapper = activationLinkMapper;
		this.activationLinkSender = activationLinkSender;
		this.readerInitializer = readerInitializer;
		this.passwordEncoder = passwordEncoder;
		this.idGenerator = idGenerator;
		this.clock = clock;
	}

	@Transactional
	public ActivationLink createReader(String email, String rawPassword) {
		final ReaderId readerId = this.readerMapper.findByEmail(email)
				.map(Reader::readerId)
				.orElseGet(() -> {
					final ReaderId id = new ReaderId(idGenerator.generateId());
					this.readerMapper.insert(id, email);
					return id;
				});
		final String hashedPassword = this.passwordEncoder.encode(rawPassword);
		this.readerPasswordMapper.deleteByReaderId(readerId);
		this.readerPasswordMapper.insert(new ReaderPassword(readerId, hashedPassword));
		final ActivationLink activationLink = new ActivationLink(new ActivationLinkId(idGenerator.generateId()), readerId, OffsetDateTime.now(this.clock));
		this.activationLinkMapper.insert(activationLink);
		log.info("sendActivationLink: {} {}", email, activationLink.activationId());
		this.activationLinkSender.sendActivationLink(email, activationLink);
		return activationLink;
	}

	@Transactional(noRollbackFor = ActivationLinkExpiredException.class)
	public ActivationLinkId activate(ActivationLink activationLink) {
		this.activationLinkMapper.deleteById(activationLink.activationId());
		if (!activationLink.isValid(this.clock)) {
			throw new ActivationLinkExpiredException();
		}
		final ReaderId readerId = activationLink.readerId();
		this.readerMapper.updateReaderState(readerId, ReaderState.ENABLED);
		this.readerInitializer.initialize(readerId);
		return activationLink.activationId();
	}
}
