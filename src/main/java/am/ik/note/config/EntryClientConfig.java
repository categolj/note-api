package am.ik.note.config;

import am.ik.note.entry.EntryClient;
import am.ik.note.entry.EntryProps;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EntryProps.class)
public class EntryClientConfig {
	@Bean
	public EntryClient entryClient(WebClient.Builder builder, EntryProps props) {
		final WebClientAdapter adapter = WebClientAdapter
				.forClient(builder.baseUrl(props.apiUrl())
						.defaultHeaders(httpHeaders -> httpHeaders
								.setBasicAuth(props.clientId(), props.clientSecret()))
						.build());
		final HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(adapter)
				.build();
		return factory.createClient(EntryClient.class);
	}

}
