package am.ik.note.reader.web;

import java.time.OffsetDateTime;
import java.util.Optional;

import am.ik.note.config.SecurityConfig;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.ReaderService;
import am.ik.note.reader.activationlink.ActivationLink;
import am.ik.note.reader.activationlink.ActivationLinkExpiredException;
import am.ik.note.reader.activationlink.ActivationLinkId;
import am.ik.note.reader.activationlink.ActivationLinkMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReaderController.class)
@Import(SecurityConfig.class)
class ReaderControllerTest {
	@Autowired
	MockMvc mockMvc;

	@MockBean
	ReaderService readerService;

	@MockBean
	ReaderMapper readerMapper;

	@MockBean
	ActivationLinkMapper activationLinkMapper;

	@Test
	void createReader_200() throws Exception {
		this.mockMvc
				.perform(post("/readers").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "demo@example.com", "rawPassword":  "password"}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.message")
						.value("Sent an activation link to demo@example.com"));
	}

	@Test
	void activate_200() throws Exception {
		final ActivationLinkId activationLinkId = ActivationLinkId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.activationLinkMapper.findById(activationLinkId))
				.willReturn(Optional.of(new ActivationLink(activationLinkId, readerId,
						OffsetDateTime.now())));
		given(this.readerService.activate(any())).willReturn(activationLinkId);
		this.mockMvc
				.perform(post("/readers/%s/activations/%s".formatted(readerId,
						activationLinkId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Activated " + activationLinkId));
	}

	@Test
	void activate_400() throws Exception {
		final ActivationLinkId activationLinkId = ActivationLinkId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.activationLinkMapper.findById(activationLinkId))
				.willReturn(Optional.of(new ActivationLink(activationLinkId, readerId,
						OffsetDateTime.now())));
		given(this.readerService.activate(any()))
				.willThrow(new ActivationLinkExpiredException());
		this.mockMvc.perform(
				post("/readers/%s/activations/%s".formatted(readerId, activationLinkId)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
						.value("The given link has been already expired."));
	}

	@Test
	void activate_404() throws Exception {
		final ActivationLinkId activationLinkId = ActivationLinkId.random();
		final ReaderId readerId = ReaderId.random();
		given(this.activationLinkMapper.findById(activationLinkId))
				.willReturn(Optional.of(new ActivationLink(activationLinkId, readerId,
						OffsetDateTime.now())));
		this.mockMvc
				.perform(post(
						"/readers/%s/activations/%s".formatted("def", activationLinkId)))
				.andExpect(status().isNotFound());
	}
}