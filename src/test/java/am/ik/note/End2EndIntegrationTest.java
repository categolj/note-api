package am.ik.note;

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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "logging.level.web=INFO", "maildev.port=0", "spring.http.client.factory=simple",
				"spring.modulith.events.republish-outstanding-events-on-restart=false" })
@Import(TestContainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(OrderAnnotation.class)
public class End2EndIntegrationTest {

	RestClient restClient;

	@LocalServerPort
	int serverPort;

	int maildevPort;

	@BeforeEach
	void setUp(@Autowired RestClient.Builder restClientBuilder, @Value("${maildev.port}") int maildevPort) {
		this.restClient = restClientBuilder.defaultStatusHandler(__ -> true, (req, res) -> {
		}).build();
		this.maildevPort = maildevPort;
		this.restClient.delete()
			.uri("http://localhost:{port}/email/all", this.maildevPort)
			.retrieve()
			.toBodilessEntity();
	}

	@Autowired
	NoteMapper noteMapper;

	@Autowired
	ReaderMapper readerMapper;

	@Autowired
	ActivationLinkMapper activationLinkMapper;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	EntryClient entryClient;

	@MockitoBean
	InfoEndpoint infoEndpoint;

	@Autowired
	JdbcClient jdbcClient;

	WebTestClient webTestClient;

	static String accessToken;

	public End2EndIntegrationTest() {
		this.webTestClient = WebTestClient.bindToServer(new JdkClientHttpConnector())
			.baseUrl("http://localhost:" + serverPort)
			.build();
	}

	@Test
	@Order(1)
	void createAccount() throws Exception {
		this.noteMapper.insertNote(NoteId.valueOf("44b04a8f-47cf-4d5f-a273-96a40fbbe8d7"), 300L,
				"https://example.com/300");
		this.noteMapper.insertNote(NoteId.valueOf("25773727-3af7-463d-b144-db089f4963d7"), 400L,
				"https://example.com/400");
		this.webTestClient.post()
			.uri("http://localhost:{port}/readers", this.serverPort)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("email", "test@example.com", "rawPassword", "mypassword"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("Sent an activation link to test@example.com");
		AtomicReference<List<Map<String, Object>>> received = new AtomicReference<>();
		Awaitility.await().until(() -> {
			received.set(this.restClient.get()
				.uri("http://localhost:{port}/email", this.maildevPort)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				}));
			return received.get() != null && received.get().size() == 1;
		});
		assertThat(received.get()).hasSize(1);
		assertThat(received.get().get(0)).containsEntry("subject", "【はじめるSpring Boot 3】アカウントアクティベーションリンク通知");
		assertThat(received.get().get(0)).containsKey("text");
		assertThat(received.get().get(0).get("text").toString())
			.contains("購読ありがとうございます。次のURLをクリックしてアカウントのアクティベートを行って下さい。");
		assertThat(received.get().get(0)).containsEntry("to",
				List.of(Map.of("address", "test@example.com", "name", "")));
		assertThat(received.get().get(0)).containsEntry("from",
				List.of(Map.of("address", "noreply@ik.am", "name", "")));
	}

	@Test
	@Order(2)
	void accountDisabled() throws Exception {
		this.webTestClient.post()
			.uri("http://localhost:{port}/oauth/token", this.serverPort)
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
			.uri("http://localhost:{port}/readers/{readerId}/activations/{activationLinkId}", this.serverPort, readerId,
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
			.uri("http://localhost:{port}/oauth/token", this.serverPort)
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
			.uri("http://localhost:{port}/notes", this.serverPort)
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
			.uri("http://localhost:{port}/notes/100", this.serverPort)
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
			.uri("http://localhost:{port}/notes/200", this.serverPort)
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
			.uri("http://localhost:{port}/notes/300", this.serverPort)
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
			.uri("http://localhost:{port}/notes/400", this.serverPort)
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
			.uri("http://localhost:{port}/notes/25773727-3af7-463d-b144-db089f4963d7/subscribe", this.serverPort)
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
			.uri("http://localhost:{port}/notes/25773727-3af7-463d-b144-db089f4963d7/subscribe", this.serverPort)
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
			.uri("http://localhost:{port}/notes/100", this.serverPort)
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
			.uri("http://localhost:{port}/notes/200", this.serverPort)
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
			.uri("http://localhost:{port}/notes/300", this.serverPort)
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
			.uri("http://localhost:{port}/notes/400", this.serverPort)
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
		this.webTestClient.post()
			.uri("http://localhost:{port}/password_reset/send_link", this.serverPort)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("email", "test@example.com"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("Sent a link.");
		{
			AtomicReference<List<Map<String, Object>>> received = new AtomicReference<>();
			Awaitility.await().until(() -> {
				received.set(this.restClient.get()
					.uri("http://localhost:{port}/email", this.maildevPort)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					}));
				return received.get() != null && received.get().size() == 1;
			});
			assertThat(received.get()).hasSize(1);
			assertThat(received.get().get(0)).containsEntry("subject", "【はじめるSpring Boot 3】パスワードリセットリンク通知");
			assertThat(received.get().get(0)).containsKey("text");
			assertThat(received.get().get(0).get("text").toString()).contains("次のURLをクリックしてパスワードリセットを行って下さい。");
			assertThat(received.get().get(0)).containsEntry("to",
					List.of(Map.of("address", "test@example.com", "name", "")));
			assertThat(received.get().get(0)).containsEntry("from",
					List.of(Map.of("address", "noreply@ik.am", "name", "")));
		}
		final String resetId = this.jdbcClient.sql("""
				SELECT reset_id FROM password_reset WHERE reader_id = ?
				""").params(readerId.toString()).query(String.class).single();
		this.webTestClient.post()
			.uri("http://localhost:{port}/password_reset", this.serverPort)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("resetId", resetId, "newPassword", "foobar"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.message")
			.isEqualTo("Reset the password");
		{
			AtomicReference<List<Map<String, Object>>> received = new AtomicReference<>();
			Awaitility.await().until(() -> {
				received.set(this.restClient.get()
					.uri("http://localhost:{port}/email", this.maildevPort)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					}));
				return received.get() != null && received.get().size() == 2;
			});
			assertThat(received.get()).hasSize(2);
			assertThat(received.get().get(1)).containsEntry("subject", "【はじめるSpring Boot 3】パスワードリセット完了通知");
			assertThat(received.get().get(1)).containsKey("text");
			assertThat(received.get().get(1).get("text").toString()).contains("パスワードがリセットされました。次のURLからログインして下さい。");
			assertThat(received.get().get(1)).containsEntry("to",
					List.of(Map.of("address", "test@example.com", "name", "")));
			assertThat(received.get().get(1)).containsEntry("from",
					List.of(Map.of("address", "noreply@ik.am", "name", "")));
		}
	}

	@Test
	@Order(9)
	void issueTokenWithOldPassword() throws Exception {
		this.webTestClient.post()
			.uri("http://localhost:{port}/oauth/token", this.serverPort)
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
			.uri("http://localhost:{port}/oauth/token", this.serverPort)
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
