package am.ik.note.entry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "entry")
public record EntryProps(String apiUrl, String clientId, String clientSecret,
		@DefaultValue("500ms") Duration retryInterval, @DefaultValue("4s") Duration retryMaxElapsedTime,
		@DefaultValue("5s") Duration connectTimeout, @DefaultValue("5s") Duration readTimeout) {
}
