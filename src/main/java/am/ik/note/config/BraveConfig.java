package am.ik.note.config;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.otel.brave.InstrumentationScope;
import zipkin2.reporter.otel.brave.OtlpProtoV1Encoder;
import zipkin2.reporter.otel.brave.TagToAttributes;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public class BraveConfig {

	@Bean
	public OtlpProtoV1Encoder otlpProtoV1Encoder(OpenTelemetryProperties properties) {
		return OtlpProtoV1Encoder.newBuilder()
			.instrumentationScope(new InstrumentationScope("org.springframework.boot", SpringBootVersion.getVersion()))
			.resourceAttributes(properties.getResourceAttributes())
			.tagToAttributes(TagToAttributes.newBuilder()
				.withDefaults()
				.tagToAttribute("method", "http.request.method")
				.tagToAttribute("status", "http.response.status_code")
				.tagToAttribute("jdbc.query[0]", "db.query.text")
				.tagToAttribute("jdbc.row-count", "db.response.returned_rows")
				.build())
			.build();
	}

	@Bean
	public Encoding otlpEncoding() {
		return Encoding.PROTO3;
	}

}