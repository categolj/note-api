package am.ik.note.config;

import am.ik.note.entry.EntryClient;
import am.ik.note.entry.EntryProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestTemplateAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EntryProps.class)
public class EntryClientConfig {
	@Bean
	public EntryClient entryClient(RestTemplateBuilder restTemplateBuilder,
			EntryProps props) {
		final RestTemplateAdapter adapter = RestTemplateAdapter.create(restTemplateBuilder
				.rootUri(props.apiUrl())
				.basicAuthentication(props.clientId(), props.clientSecret()).build());
		final HttpServiceProxyFactory factory = HttpServiceProxyFactory
				.builderFor(adapter).build();
		return factory.createClient(EntryClient.class);
	}

}
