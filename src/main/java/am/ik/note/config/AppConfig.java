package am.ik.note.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AppConfig {

	@Bean
	AccessLogger accessLogger() {
		return AccessLogger.builder().filter(httpExchange -> {
			String uri = httpExchange.getRequest().getUri().getPath();
			return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator")
					|| uri.startsWith("/_static"));
		}).addKeyValues(true).build();
	}

}
