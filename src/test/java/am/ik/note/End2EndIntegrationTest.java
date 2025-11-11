package am.ik.note;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import am.ik.note.content.NoteId;
import am.ik.note.content.NoteMapper;
import am.ik.note.entry.Author;
import am.ik.note.entry.Entries;
import am.ik.note.entry.Entry;
import am.ik.note.entry.EntryClient;
import am.ik.note.entry.FrontMatter;
import am.ik.note.reader.Reader;
import am.ik.note.reader.ReaderId;
import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.activationlink.ActivationLinkMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGridAPI;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "logging.level.web=INFO")
@Import(TestContainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(OrderAnnotation.class)
// @AutoConfigureObservability
public class End2EndIntegrationTest {

	@LocalServerPort
	int port;

	@Autowired
	NoteMapper noteMapper;

	@Autowired
	ReaderMapper readerMapper;

	@Autowired
	ActivationLinkMapper activationLinkMapper;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	EntryClient entryClient;

	@MockBean
	SendGridAPI sendGrid;

	@MockBean
	InfoEndpoint infoEndpoint;

	@Autowired
	JdbcClient jdbcClient;

	WebTestClient webTestClient;

	static String accessToken;

	public End2EndIntegrationTest() {
		this.webTestClient = WebTestClient.bindToServer(new JdkClientHttpConnector())
			.baseUrl("http://localhost:" + port)
			.build();
	}

	@Test
	@Order(1)
	void createAccount() throws Exception {
		given(this.sendGrid.api(any())).willReturn(new Response(202, "OK", Map.of()));
		this.noteMapper.insertNote(NoteId.valueOf("44b04a8f-47cf-4d5f-a273-96a40fbbe8d7"), 300L,
				"https://example.com/300");
		this.noteMapper.insertNote(NoteId.valueOf("25773727-3af7-463d-b144-db089f4963d7"), 400L,
				"https://example.com/400");
		this.webTestClient.post()
			.uri("http://localhost:{port}/readers", this.port)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("email", "test@example.com", "rawPassword", "mypassword"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("Sent an activation link to test@example.com");
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(this.sendGrid).api(captor.capture());
		assertThat(captor.getValue().getBody()).contains("アカウントアクティベーションリンク通知").contains("test@example.com");
	}

	@Test
	@Order(2)
	void accountDisabled() throws Exception {
		this.webTestClient.post()
			.uri("http://localhost:{port}/oauth/token", this.port)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.bodyValue(new LinkedMultiValueMap<>(
					Map.of("username", List.of("test@example.com"), "password", List.of("mypassword"))))
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Test
	@Order(3)
	void activateAccount() {
		var row = this.jdbcClient.sql("""
				SELECT al.activation_id, al.reader_id
				FROM activation_link AS al, reader AS r
				WHERE al.reader_id = r.reader_id AND r.email = ?""").params("test@example.com").query().singleRow();
		String activationId = (String) row.get("activation_id");
		String readerId = (String) row.get("reader_id");
		this.webTestClient.post()
			.uri("http://localhost:{port}/readers/{readerId}/activations/{activationLinkId}", this.port, readerId,
					activationId)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("Activated " + activationId);
	}

	@Test
	@Order(4)
	void issueToken() throws Exception {
		final EntityExchangeResult<byte[]> tokenResult = this.webTestClient.post()
			.uri("http://localhost:{port}/oauth/token", this.port)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.bodyValue(new LinkedMultiValueMap<>(
					Map.of("username", List.of("test@example.com"), "password", List.of("mypassword"))))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.expires_in")
			.isEqualTo(43200)
			.jsonPath("$.token_type")
			.isEqualTo("Bearer")
			.jsonPath("$.access_token")
			.isNotEmpty()
			.returnResult();
		final JsonNode tokenNode = this.objectMapper.readValue(tokenResult.getResponseBody(), JsonNode.class);
		accessToken = tokenNode.get("access_token").asText();
	}

	@Test
	@Order(5)
	void checkAvailableNoteList() {
		given(this.entryClient.getEntries()).willReturn(new Entries(List.of(
				new Entry(100L, new FrontMatter("entry 100"), null, null, new Author("admin", OffsetDateTime.now())),
				new Entry(200L, new FrontMatter("entry 200"), null, null, new Author("admin", OffsetDateTime.now())),
				new Entry(300L, new FrontMatter("entry 300"), null, null, new Author("admin", OffsetDateTime.now())),
				new Entry(400L, new FrontMatter("entry 400"), null, null, new Author("admin", OffsetDateTime.now())))));
		this.webTestClient.get()
			.uri("http://localhost:{port}/notes", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.length()")
			.isEqualTo(4)
			.jsonPath("$[0].entryId")
			.isEqualTo(100)
			.jsonPath("$[0].title")
			.isEqualTo("entry 100")
			.jsonPath("$[0].noteUrl")
			.isNotEmpty()
			.jsonPath("$[0].subscribed")
			.isEqualTo(true)
			.jsonPath("$[1].entryId")
			.isEqualTo(200)
			.jsonPath("$[1].title")
			.isEqualTo("entry 200")
			.jsonPath("$[1].noteUrl")
			.isNotEmpty()
			.jsonPath("$[1].subscribed")
			.isEqualTo(true)
			.jsonPath("$[2].entryId")
			.isEqualTo(300)
			.jsonPath("$[2].title")
			.isEqualTo("entry 300")
			.jsonPath("$[2].noteUrl")
			.isEqualTo("https://example.com/300")
			.jsonPath("$[2].subscribed")
			.isEqualTo(false)
			.jsonPath("$[3].entryId")
			.isEqualTo(400)
			.jsonPath("$[3].title")
			.isEqualTo("entry 400")
			.jsonPath("$[3].noteUrl")
			.isEqualTo("https://example.com/400")
			.jsonPath("$[3].subscribed")
			.isEqualTo(false);
	}

	@Test
	@Order(5)
	void checkAvailableNote() {
		List.of(new Entry(100L, new FrontMatter("entry 100"), null, null, null),
				new Entry(200L, new FrontMatter("entry 200"), null, null, null),
				new Entry(300L, new FrontMatter("entry 300"), null, null, null),
				new Entry(400L, new FrontMatter("entry 400"), null, null, null))
			.forEach(entry -> given(this.entryClient.getEntry(entry.entryId())).willReturn(entry));

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/100", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.entryId")
			.isEqualTo(100)
			.jsonPath("$.noteUrl")
			.isNotEmpty()
			.jsonPath("$.noteId")
			.doesNotExist()
			.jsonPath("$.frontMatter.title")
			.isEqualTo("entry 100");

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/200", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.entryId")
			.isEqualTo(200)
			.jsonPath("$.noteUrl")
			.isNotEmpty()
			.jsonPath("$.noteId")
			.doesNotExist()
			.jsonPath("$.frontMatter.title")
			.isEqualTo("entry 200");

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/300", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isForbidden()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("You are not allowed to access to the entry.")
			.jsonPath("$.noteUrl")
			.isEqualTo("https://example.com/300");

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/400", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isForbidden()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("You are not allowed to access to the entry.")
			.jsonPath("$.noteUrl")
			.isEqualTo("https://example.com/400");
	}

	@Test
	@Order(6)
	void subscribe() {
		this.webTestClient.post()
			.uri("http://localhost:{port}/notes/25773727-3af7-463d-b144-db089f4963d7/subscribe", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.entryId")
			.isEqualTo(400)
			.jsonPath("$.subscribed")
			.isEqualTo(false);

		this.webTestClient.post()
			.uri("http://localhost:{port}/notes/25773727-3af7-463d-b144-db089f4963d7/subscribe", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.entryId")
			.isEqualTo(400)
			.jsonPath("$.subscribed")
			.isEqualTo(true);
	}

	@Test
	@Order(7)
	void checkAvailableNoteAgain() {
		List.of(new Entry(100L, new FrontMatter("entry 100"), null, null, null),
				new Entry(200L, new FrontMatter("entry 200"), null, null, null),
				new Entry(300L, new FrontMatter("entry 300"), null, null, null),
				new Entry(400L, new FrontMatter("entry 400"), null, null, null))
			.forEach(entry -> given(this.entryClient.getEntry(entry.entryId())).willReturn(entry));

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/100", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.entryId")
			.isEqualTo(100)
			.jsonPath("$.noteUrl")
			.isNotEmpty()
			.jsonPath("$.noteId")
			.doesNotExist()
			.jsonPath("$.frontMatter.title")
			.isEqualTo("entry 100");

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/200", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.entryId")
			.isEqualTo(200)
			.jsonPath("$.noteUrl")
			.isNotEmpty()
			.jsonPath("$.noteId")
			.doesNotExist()
			.jsonPath("$.frontMatter.title")
			.isEqualTo("entry 200");

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/300", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isForbidden()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("You are not allowed to access to the entry.")
			.jsonPath("$.noteUrl")
			.isEqualTo("https://example.com/300");

		this.webTestClient.get()
			.uri("http://localhost:{port}/notes/400", this.port)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.entryId")
			.isEqualTo(400)
			.jsonPath("$.noteUrl")
			.isEqualTo("https://example.com/400")
			.jsonPath("$.noteId")
			.doesNotExist()
			.jsonPath("$.frontMatter.title")
			.isEqualTo("entry 400");
	}

	@Test
	@Order(8)
	void resetPassword() throws Exception {
		final Reader reader = this.readerMapper.findByEmail("test@example.com").orElseThrow();
		final ReaderId readerId = reader.readerId();
		given(this.sendGrid.api(any())).willReturn(new Response(202, "OK", Map.of()));
		this.webTestClient.post()
			.uri("http://localhost:{port}/password_reset/send_link", this.port)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("email", "test@example.com"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("Sent a link.");
		{
			Thread.sleep(100);
			ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
			verify(this.sendGrid).api(captor.capture());
			assertThat(captor.getValue().getBody()).contains("パスワードリセットリンク通知").contains("test@example.com");
		}
		final String resetId = this.jdbcClient.sql("""
				SELECT reset_id FROM password_reset WHERE reader_id = ?
				""").params(readerId.toString()).query(String.class).single();
		this.webTestClient.post()
			.uri("http://localhost:{port}/password_reset", this.port)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("resetId", resetId, "newPassword", "foobar"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("Reset the password");
		{
			Thread.sleep(100);
			ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
			verify(this.sendGrid, times(2)).api(captor.capture());
			assertThat(captor.getValue().getBody()).contains("パスワードリセット完了通知").contains("test@example.com");
		}
	}

	@Test
	@Order(9)
	void issueTokenWithOldPassword() throws Exception {
		this.webTestClient.post()
			.uri("http://localhost:{port}/oauth/token", this.port)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.bodyValue(new LinkedMultiValueMap<>(
					Map.of("username", List.of("test@example.com"), "password", List.of("mypassword"))))
			.exchange()
			.expectStatus()
			.isUnauthorized()
			.expectBody()
			.jsonPath("$.error")
			.isEqualTo("unauthorized");
	}

	@Test
	@Order(10)
	void issueTokenWithNewPassword() throws Exception {
		final EntityExchangeResult<byte[]> tokenResult = this.webTestClient.post()
			.uri("http://localhost:{port}/oauth/token", this.port)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.bodyValue(new LinkedMultiValueMap<>(
					Map.of("username", List.of("test@example.com"), "password", List.of("foobar"))))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.expires_in")
			.isEqualTo(43200)
			.jsonPath("$.token_type")
			.isEqualTo("Bearer")
			.jsonPath("$.access_token")
			.isNotEmpty()
			.returnResult();
		final JsonNode tokenNode = this.objectMapper.readValue(tokenResult.getResponseBody(), JsonNode.class);
		accessToken = tokenNode.get("access_token").asText();
		this.cleanUp();
	}

	// @AfterEach
	void cleanUp() {
		this.noteMapper.deleteByEntryId(300L);
		this.noteMapper.deleteByEntryId(400L);
		this.readerMapper.deleteByEmail("test@example.com");
	}

}
