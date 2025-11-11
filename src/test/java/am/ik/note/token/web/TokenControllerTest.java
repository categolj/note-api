package am.ik.note.token.web;

import am.ik.note.MockConfig;
import am.ik.note.config.SecurityConfig;
import am.ik.note.reader.Reader;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.ReaderState;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TokenController.class)
@Import({ MockConfig.class, SecurityConfig.class })
class TokenControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ReaderMapper readerMapper;

	@Test
	void token_200_by_email() throws Exception {
		final ReaderId readerId = ReaderId.random();
		given(this.readerMapper.findByEmail("demo@example.com"))
			.willReturn(Optional.of(new Reader(readerId, "demo@example.com", "{noop}password", ReaderState.ENABLED)));
		this.mockMvc
			.perform(post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("username", "demo@example.com")
				.param("password", "password"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.access_token").isNotEmpty())
			.andExpect(jsonPath("$.token_type").value("Bearer"))
			.andExpect(jsonPath("$.expires_in").value(43200))
			.andExpect(jsonPath("$.scope.length()").value(2))
			.andExpect(jsonPath("$.scope[0]").value("note:read"))
			.andExpect(jsonPath("$.scope[1]").value("openid"));
	}

	@Test
	void token_200_by_readerId() throws Exception {
		final ReaderId readerId = ReaderId.random();
		given(this.readerMapper.findById(readerId))
			.willReturn(Optional.of(new Reader(readerId, "demo@example.com", "{noop}password", ReaderState.ENABLED)));
		this.mockMvc
			.perform(post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("username", readerId.toString())
				.param("password", "password"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.access_token").isNotEmpty())
			.andExpect(jsonPath("$.token_type").value("Bearer"))
			.andExpect(jsonPath("$.expires_in").value(43200))
			.andExpect(jsonPath("$.scope.length()").value(2))
			.andExpect(jsonPath("$.scope[0]").value("note:read"))
			.andExpect(jsonPath("$.scope[1]").value("openid"));
	}

	@Test
	void token_401_bad_username() throws Exception {
		given(this.readerMapper.findByEmail("demo@example.com")).willReturn(Optional.empty());
		this.mockMvc
			.perform(post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("username", "demo@example.com")
				.param("password", "password"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("unauthorized"))
			.andExpect(jsonPath("$.error_description").value("Bad credentials"));
	}

	@Test
	void token_401_bad_password() throws Exception {
		final ReaderId readerId = ReaderId.random();
		given(this.readerMapper.findByEmail("demo@example.com"))
			.willReturn(Optional.of(new Reader(readerId, "demo@example.com", "{noop}password", ReaderState.ENABLED)));
		this.mockMvc
			.perform(post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("username", "demo@example.com")
				.param("password", "badpassword"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("unauthorized"))
			.andExpect(jsonPath("$.error_description").value("Bad credentials"));
	}

	@Test
	void token_401_disabled() throws Exception {
		final ReaderId readerId = ReaderId.random();
		given(this.readerMapper.findByEmail("demo@example.com"))
			.willReturn(Optional.of(new Reader(readerId, "demo@example.com", "{noop}password", ReaderState.DISABLED)));
		this.mockMvc
			.perform(post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("username", "demo@example.com")
				.param("password", "password"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("unauthorized"))
			.andExpect(jsonPath("$.error_description").value("User is disabled"));
	}

	@Test
	void token_401_locked() throws Exception {
		final ReaderId readerId = ReaderId.random();
		given(this.readerMapper.findByEmail("demo@example.com"))
			.willReturn(Optional.of(new Reader(readerId, "demo@example.com", "{noop}password", ReaderState.LOCKED)));
		this.mockMvc
			.perform(post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("username", "demo@example.com")
				.param("password", "password"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("unauthorized"))
			.andExpect(jsonPath("$.error_description").value("User account is locked"));
	}

}
