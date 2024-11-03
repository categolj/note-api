package am.ik.note.entry;

import am.ik.note.config.EntryClientConfig;
import am.ik.spring.logbook.AccessLoggerLogbookAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(properties = { "logging.level.web=INFO", "entry.retry-interval=5ms",
		"entry.retry-max-elapsed-time=40ms", "entry.api-url=https://example.com" })
@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
		AccessLoggerLogbookAutoConfiguration.class, LogbookAutoConfiguration.class })
@Import(EntryClientConfig.class)
class EntryClientTest {
	@Autowired
	EntryClient entryClient;

	@Autowired
	MockRestServiceServer server;

	@Test
	void getEntries() {
		this.server.expect(requestTo("/entries"))
				.andRespond(withSuccess(
						"""
								{
								  "content": [
								    {
								      "entryId": 750,
								      "frontMatter": {
								        "title": "Installing Postfacto",
								        "categories": [
								          {
								            "name": "Dev"
								          },
								          {
								            "name": "Retrospectives"
								          },
								          {
								            "name": "Postfacto"
								          }
								        ],
								        "tags": [
								          {
								            "name": "Helm"
								          },
								          {
								            "name": "Kubernetes"
								          },
								          {
								            "name": "Postfacto"
								          }
								        ]
								      },
								      "content": "",
								      "created": {
								        "name": "Toshiaki Maki",
								        "date": "2023-07-11T10:10:24Z"
								      },
								      "updated": {
								        "name": "Toshiaki Maki",
								        "date": "2023-07-23T17:18:18Z"
								      }
								    },
								    {
								      "entryId": 747,
								      "frontMatter": {
								        "title": "Brainf*ck compiler to WebAssembly in Java",
								        "categories": [
								          {
								            "name": "Programming"
								          },
								          {
								            "name": "WebAssembly"
								          },
								          {
								            "name": "Brainf*ck"
								          }
								        ],
								        "tags": [
								          {
								            "name": "Brainf*ck"
								          },
								          {
								            "name": "Java"
								          },
								          {
								            "name": "Wasm Workers Server"
								          },
								          {
								            "name": "WebAssembly"
								          }
								        ]
								      },
								      "content": "",
								      "created": {
								        "name": "Toshiaki Maki",
								        "date": "2023-07-02T04:20:47Z"
								      },
								      "updated": {
								        "name": "Toshiaki Maki",
								        "date": "2023-07-18T08:02:21Z"
								      }
								    },
								    {
								      "entryId": 738,
								      "frontMatter": {
								        "title": "Installing Tanzu Application Platform 1.5 (Iterate Profile) on kind",
								        "categories": [
								          {
								            "name": "Dev"
								          },
								          {
								            "name": "CaaS"
								          },
								          {
								            "name": "Kubernetes"
								          },
								          {
								            "name": "TAP"
								          }
								        ],
								        "tags": [
								          {
								            "name": "Cartographer"
								          },
								          {
								            "name": "Kubernetes"
								          },
								          {
								            "name": "TAP"
								          },
								          {
								            "name": "Tanzu"
								          },
								          {
								            "name": "kind"
								          }
								        ]
								      },
								      "content": "",
								      "created": {
								        "name": "Toshiaki Maki",
								        "date": "2023-04-11T18:49:27Z"
								      },
								      "updated": {
								        "name": "Toshiaki Maki",
								        "date": "2023-07-18T07:53:22Z"
								      }
								    }
								  ],
								  "size": 3,
								  "number": 0,
								  "totalElements": 613
								}
								""",
						MediaType.APPLICATION_JSON));
		Entries entries = entryClient.getEntries();
		assertThat(entries).isNotNull();
		List<Entry> content = entries.content();
		assertThat(content).hasSize(3);
		assertThat(content.get(0).entryId()).isEqualTo(750);
		assertThat(content.get(0).frontMatter()).isNotNull();
		assertThat(content.get(0).frontMatter().title())
				.isEqualTo("Installing Postfacto");
		assertThat(content.get(0).content()).isEqualTo("");
		assertThat(content.get(0).created()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-07-11T10:10:24Z")));
		assertThat(content.get(0).updated()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-07-23T17:18:18Z")));
		assertThat(content.get(1).entryId()).isEqualTo(747);
		assertThat(content.get(1).frontMatter()).isNotNull();
		assertThat(content.get(1).frontMatter().title())
				.isEqualTo("Brainf*ck compiler to WebAssembly in Java");
		assertThat(content.get(1).content()).isEqualTo("");
		assertThat(content.get(1).created()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-07-02T04:20:47Z")));
		assertThat(content.get(1).updated()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-07-18T08:02:21Z")));
		assertThat(content.get(2).entryId()).isEqualTo(738);
		assertThat(content.get(2).frontMatter()).isNotNull();
		assertThat(content.get(2).frontMatter().title()).isEqualTo(
				"Installing Tanzu Application Platform 1.5 (Iterate Profile) on kind");
		assertThat(content.get(2).content()).isEqualTo("");
		assertThat(content.get(2).created()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-04-11T18:49:27Z")));
		assertThat(content.get(2).updated()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-07-18T07:53:22Z")));
	}

	@Test
	void getEntry() {
		this.server.expect(requestTo("/entries/751"))
				.andRespond(withSuccess(
						"""
								{
								  "entryId": 751,
								  "frontMatter": {
								    "title": "How to force remove a namespace",
								    "categories": [
								      {
								        "name": "Dev"
								      },
								      {
								        "name": "CaaS"
								      },
								      {
								        "name": "Kubernetes"
								      }
								    ],
								    "tags": [
								      {
								        "name": "Kubernetes"
								      }
								    ]
								  },
								  "content": "memo\\n\\n```\\nexport NS=hogehoge && echo '{\\"metadata\\":{\\"name\\":\\"'$NS'\\"},\\"spec\\":{\\"finalizers\\":[]}}' | kubectl replace --raw \\"/api/v1/namespaces/$NS/finalize\\" -f -\\n```",
								  "created": {
								    "name": "Toshiaki Maki",
								    "date": "2023-07-12T07:51:48Z"
								  },
								  "updated": {
								    "name": "Toshiaki Maki",
								    "date": "2023-07-12T07:51:48Z"
								  }
								}
								""",
						MediaType.APPLICATION_JSON));
		Entry entry = entryClient.getEntry(751L);
		assertThat(entry).isNotNull();
		assertThat(entry.entryId()).isEqualTo(751L);
		assertThat(entry.content()).isEqualTo(
				"""
						memo

						```
						export NS=hogehoge && echo '{"metadata":{"name":"'$NS'"},"spec":{"finalizers":[]}}' | kubectl replace --raw "/api/v1/namespaces/$NS/finalize" -f -
						```
						"""
						.trim());
		assertThat(entry.created()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-07-12T07:51:48Z")));
		assertThat(entry.updated()).isEqualTo(new Author("Toshiaki Maki",
				OffsetDateTime.parse("2023-07-12T07:51:48Z")));
	}

	@Test
	void getEntryRetryWithSuccess() {
		this.server.expect(requestTo("/entries/751")).andRespond(withServerError());
		this.server.expect(requestTo("/entries/751")).andRespond(withBadGateway());
		this.server.expect(requestTo("/entries/751"))
				.andRespond(withServiceUnavailable());
		this.server.expect(requestTo("/entries/751")).andRespond(withGatewayTimeout());
		this.server.expect(requestTo("/entries/751")).andRespond(withSuccess("""
				{"entryId": 751}
				""", MediaType.APPLICATION_JSON));
		Entry entry = this.entryClient.getEntry(751L);
		assertThat(entry).isNotNull();
		assertThat(entry.entryId()).isEqualTo(751L);
	}

	@Test
	void getEntryRetryTimeoutWithSuccess() {
		this.server.expect(ExpectedCount.times(4), requestTo("/entries/751"))
				.andRespond(withException(new SocketTimeoutException("timeout")));
		this.server.expect(requestTo("/entries/751")).andRespond(withSuccess("""
				{"entryId": 751}
				""", MediaType.APPLICATION_JSON));
		Entry entry = this.entryClient.getEntry(751L);
		assertThat(entry).isNotNull();
		assertThat(entry.entryId()).isEqualTo(751L);
	}

	@Test
	void getEntryRetryWithFailure() {
		this.server.expect(requestTo("/entries/751")).andRespond(withServerError());
		this.server.expect(requestTo("/entries/751")).andRespond(withBadGateway());
		this.server.expect(requestTo("/entries/751"))
				.andRespond(withServiceUnavailable());
		this.server.expect(requestTo("/entries/751")).andRespond(withGatewayTimeout());
		this.server.expect(requestTo("/entries/751"))
				.andRespond(withTooManyRequests().body("rate limit exceeded!"));
		this.server.expect(requestTo("/entries/751")).andRespond(withSuccess("""
				{"entryId": 751}
				""", MediaType.APPLICATION_JSON));
		assertThatThrownBy(() -> {
			this.entryClient.getEntry(751L);
		}).isInstanceOf(HttpClientErrorException.class)
				.hasMessage("429 Too Many Requests: \"rate limit exceeded!\"");
	}

	@Test
	void getEntryRetryTimeoutWithFailure() {
		this.server.expect(ExpectedCount.times(5), requestTo("/entries/751"))
				.andRespond(withException(new SocketTimeoutException("timeout")));
		this.server.expect(requestTo("/entries/751")).andRespond(withSuccess("""
				{"entryId": 751}
				""", MediaType.APPLICATION_JSON));
		assertThatThrownBy(() -> {
			this.entryClient.getEntry(751L);
		}).isInstanceOf(ResourceAccessException.class).hasMessage(
				"I/O error on GET request for \"https://example.com/entries/751\": timeout");
	}

	@Test
	void getEntryNotFound() {
		this.server.expect(requestTo("/entries/751"))
				.andRespond(withResourceNotFound().body("""
						{
						  "type": "about:blank",
						  "title": "Not Found",
						  "status": 404,
						  "detail": "The requested entry is not found (entryId = 751)",
						  "instance": "/entries/751",
						  "traceId": "047d8e5de270c875a6e28eb79bf9177f"
						}
						"""));
		assertThatThrownBy(() -> {
			this.entryClient.getEntry(751L);
		}).isInstanceOf(HttpClientErrorException.class)
				.hasMessageStartingWith("404 Not Found:");
	}
}