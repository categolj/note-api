package am.ik.note.sendgrid;

import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SendGridSender {

	private final Logger log = LoggerFactory.getLogger(SendGridSender.class);

	private final RestClient restClient;

	public SendGridSender(RestClient.Builder restClientBuilder, SendGridProps props) {
		this.restClient = restClientBuilder.baseUrl(props.baseUrl())
			.defaultHeaders(headers -> headers.setBearerAuth(props.apiKey()))
			.defaultStatusHandler(__ -> true, (req, res) -> {
			})
			.build();
	}

	@Observed
	public void sendMail(String to, String subject, String content) {
		log.info("Sending mail to {} subject {}", to, subject);
		ResponseEntity<String> response = this.restClient.post()
			.uri("/v3/mail/send")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("personalizations", List.of(Map.of("to", List.of(Map.of("email", to)), "subject", subject)), //
					"from", Map.of("email", "noreply@ik.am"), //
					"reply_to", Map.of("email", "makingx+hajiboot3@gmail.com"), //
					"content", List.of(Map.of("type", "text/plain", "value", content))))
			.retrieve()
			.toEntity(String.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			log.warn("statusCode = {}", response.getStatusCode());
			log.warn("headers = {}", response.getHeaders());
			log.warn("body = {}", response.getBody());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Failed to send a mail: " + response.getBody());
		}
	}

}