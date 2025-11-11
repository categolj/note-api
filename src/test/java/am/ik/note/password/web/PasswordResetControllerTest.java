package am.ik.note.password.web;

import java.time.OffsetDateTime;
import java.util.Optional;

import am.ik.note.config.SecurityConfig;
import am.ik.note.password.PasswordReset;
import am.ik.note.password.PasswordResetExpiredException;
import am.ik.note.password.PasswordResetId;
import am.ik.note.password.PasswordResetMapper;
import am.ik.note.password.PasswordResetService;
import am.ik.note.reader.Reader;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.ReaderState;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.IdGenerator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { PasswordResetController.class })
@Import(SecurityConfig.class)
class PasswordResetControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	PasswordResetService passwordResetService;

	@MockBean
	PasswordResetMapper passwordResetMapper;

	@MockBean
	ReaderMapper readerMapper;

	@MockBean
	IdGenerator idGenerator;

	@Test
	void sendLink_200() throws Exception {
		given(this.readerMapper.findByEmail("demo@example.com"))
			.willReturn(Optional.of(new Reader(ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"),
					"demo@example.com", "{noop}password", ReaderState.DISABLED)));
		this.mockMvc.perform(post("/password_reset/send_link").contentType(MediaType.APPLICATION_JSON).content("""
				{"email":  "demo@example.com"}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Sent a link."));
	}

	@Test
	void sendLink_404() throws Exception {
		given(this.readerMapper.findByEmail("demo@example.com")).willReturn(Optional.empty());
		this.mockMvc.perform(post("/password_reset/send_link").contentType(MediaType.APPLICATION_JSON).content("""
				{"email":  "demo@example.com"}
				""")).andExpect(status().isNotFound());
	}

	@Test
	void reset_200() throws Exception {
		final PasswordResetId resetId = PasswordResetId.random();
		given(this.passwordResetMapper.findByResetId(resetId)).willReturn(Optional.of(new PasswordReset(resetId,
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"), OffsetDateTime.now())));
		this.mockMvc.perform(post("/password_reset").contentType(MediaType.APPLICATION_JSON).content("""
				{"resetId": "%s", "newPassword":  "password"}
				""".formatted(resetId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Reset the password"));
	}

	@Test
	void reset_400() throws Exception {
		final PasswordResetId resetId = PasswordResetId.random();
		given(this.passwordResetMapper.findByResetId(resetId)).willReturn(Optional.of(new PasswordReset(resetId,
				ReaderId.valueOf("c872edeb-1d86-4c1a-81ac-895ace606ec4"), OffsetDateTime.now())));
		given(this.passwordResetService.reset(any(), any())).willThrow(new PasswordResetExpiredException());
		this.mockMvc.perform(post("/password_reset").contentType(MediaType.APPLICATION_JSON).content("""
				{"resetId": "%s", "newPassword":  "password"}
				""".formatted(resetId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("The given link has been already expired."));
	}

	@Test
	void reset_404() throws Exception {
		final PasswordResetId resetId = PasswordResetId.random();
		given(this.passwordResetMapper.findByResetId(resetId)).willReturn(Optional.empty());
		this.mockMvc.perform(post("/password_reset").contentType(MediaType.APPLICATION_JSON).content("""
				{"resetId": "%s", "newPassword":  "password"}
				""".formatted(resetId))).andExpect(status().isNotFound());
	}

}