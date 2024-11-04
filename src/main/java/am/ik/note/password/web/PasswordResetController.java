package am.ik.note.password.web;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import am.ik.note.common.ResponseMessage;
import am.ik.note.password.PasswordReset;
import am.ik.note.password.PasswordResetExpiredException;
import am.ik.note.password.PasswordResetId;
import am.ik.note.password.PasswordResetMapper;
import am.ik.note.password.PasswordResetService;
import am.ik.note.reader.ReaderMapper;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/password_reset")
@Tag(name = "password-reset")
public class PasswordResetController {
	private final PasswordResetService passwordResetService;

	private final PasswordResetMapper passwordResetMapper;

	private final ReaderMapper readerMapper;

	private final IdGenerator idGenerator;

	public PasswordResetController(PasswordResetService passwordResetService,
			PasswordResetMapper passwordResetMapper, ReaderMapper readerMapper,
			IdGenerator idGenerator) {
		this.passwordResetService = passwordResetService;
		this.passwordResetMapper = passwordResetMapper;
		this.readerMapper = readerMapper;
		this.idGenerator = idGenerator;
	}

	@PostMapping(path = "/send_link")
	@Transactional
	public ResponseEntity<ResponseMessage> sendLink(@RequestBody SendLinkInput input) {
		final String email = input.email();
		return ResponseEntity.of(this.readerMapper.findByEmail(email)
				.map(reader -> new PasswordReset(
						new PasswordResetId(idGenerator.generateId()), reader.readerId(),
						OffsetDateTime.now()))
				.map(this.passwordResetService::sendLink)
				.map(count -> new ResponseMessage("Sent a link.")));
	}

	@PostMapping(path = "")
	public ResponseEntity<ResponseMessage> reset(@RequestBody PasswordResetInput input) {
		try {
			return ResponseEntity
					.of(this.passwordResetMapper.findByResetId(input.toPasswordResetId())
							.map(passwordReset -> this.passwordResetService
									.reset(passwordReset, input.newPassword()))
							.map(count -> new ResponseMessage("Reset the password")));
		}
		catch (PasswordResetExpiredException e) {
			return ResponseEntity.badRequest().body(
					new ResponseMessage("The given link has been already expired."));
		}
	}

	public record SendLinkInput(String email) {

	}

	public record PasswordResetInput(UUID resetId, String newPassword) {
		PasswordResetId toPasswordResetId() {
			return new PasswordResetId(resetId);
		}
	}
}