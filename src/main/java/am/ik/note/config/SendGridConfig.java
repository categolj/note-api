package am.ik.note.config;

import am.ik.note.sendgrid.SendGridProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SendGridProps.class)
public class SendGridConfig {

}
