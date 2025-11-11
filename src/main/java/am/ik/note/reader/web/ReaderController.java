package am.ik.note.reader.web;

import java.util.Objects;

import am.ik.note.common.ResponseMessage;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderService;
import am.ik.note.reader.activationlink.ActivationLinkExpiredException;
import am.ik.note.reader.activationlink.ActivationLinkId;
import am.ik.note.reader.activationlink.ActivationLinkMapper;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/readers")
@Tag(name = "reader")
public class ReaderController {

	private final ReaderService readerService;

	private final ActivationLinkMapper activationLinkMapper;

	public ReaderController(ReaderService readerService, ActivationLinkMapper activationLinkMapper) {
		this.readerService = readerService;
		this.activationLinkMapper = activationLinkMapper;
	}

	@PostMapping(path = "")
	public ResponseEntity<ResponseMessage> createReader(@RequestBody CreateReaderInput input) {
		this.readerService.createReader(input.email(), input.rawPassword());
		return ResponseEntity.ok(new ResponseMessage("Sent an activation link to " + input.email()));
	}

	@PostMapping(
			path = "{readerId:[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}}/activations/{activationLinkId}")
	@ApiResponses({
			@ApiResponse(responseCode = "200",
					content = @Content(schema = @Schema(implementation = ResponseMessage.class))),
			@ApiResponse(responseCode = "403",
					content = @Content(schema = @Schema(implementation = ResponseMessage.class))) })
	public ResponseEntity<ResponseMessage> activate(@PathVariable("readerId") ReaderId readerId,
			@PathVariable("activationLinkId") ActivationLinkId activationLinkId) {
		try {
			return ResponseEntity.of(this.activationLinkMapper.findById(activationLinkId)
				.filter(activationLink -> Objects.equals(activationLink.readerId(), readerId))
				.map(this.readerService::activate)
				.map(id -> new ResponseMessage("Activated " + id)));
		}
		catch (ActivationLinkExpiredException e) {
			return ResponseEntity.badRequest().body(new ResponseMessage("The given link has been already expired."));
		}
	}

	record CreateReaderInput(String email, String rawPassword) {
	}

}