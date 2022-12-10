package am.ik.note.reader.web;

import java.util.Map;
import java.util.Objects;

import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderService;
import am.ik.note.reader.activationlink.ActivationLinkExpiredException;
import am.ik.note.reader.activationlink.ActivationLinkId;
import am.ik.note.reader.activationlink.ActivationLinkMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/readers")
public class ReaderController {
	private final ReaderService readerService;

	private final ActivationLinkMapper activationLinkMapper;

	public ReaderController(ReaderService readerService, ActivationLinkMapper activationLinkMapper) {
		this.readerService = readerService;
		this.activationLinkMapper = activationLinkMapper;
	}

	@PostMapping(path = "")
	public ResponseEntity<?> createReader(@RequestBody CreateReaderInput input) {
		this.readerService.createReader(input.email(), input.rawPassword());
		return ResponseEntity.ok(Map.of("message", "Sent an activation link to " + input.email()));
	}

	@PostMapping(path = "{readerId:[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}}/activations/{activationLinkId}")
	public ResponseEntity<?> activate(@PathVariable("readerId") ReaderId readerId, @PathVariable("activationLinkId") ActivationLinkId activationLinkId) {
		try {
			return ResponseEntity.of(this.activationLinkMapper.findById(activationLinkId)
					.filter(activationLink -> Objects.equals(activationLink.readerId(), readerId))
					.map(this.readerService::activate)
					.map(id -> Map.of("message", "Activated " + id)));
		}
		catch (ActivationLinkExpiredException e) {
			return ResponseEntity.badRequest().body(Map.of("message", "The given link has been already expired."));
		}
	}

	record CreateReaderInput(String email, String rawPassword) {
	}
}