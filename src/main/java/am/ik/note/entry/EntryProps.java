package am.ik.note.entry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entry")
public record EntryProps(String apiUrl, String clientId, String clientSecret) {
}
