package am.ik.note.sendgrid;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "spring.sendgrid")
public record SendGridProps(String apiKey, @DefaultValue("https://api.sendgrid.com") URI baseUrl) {

}