package am.ik.note.config;

import am.ik.note.entry.EntryClient;
import am.ik.note.entry.EntryProps;
import am.ik.spring.http.client.RetryableClientHttpRequestInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EntryProps.class)
public class EntryClientConfig {

	@Bean
	RestClientCustomizer restClientCustomizer(EntryProps props) {
		final ExponentialBackOff backOff = new ExponentialBackOff(props.retryInterval().toMillis(), 2);
		backOff.setMaxElapsedTime(props.retryMaxElapsedTime().toMillis());
		return builder -> builder.requestInterceptor(new RetryableClientHttpRequestInterceptor(backOff));
	}

	@Bean
	EntryClient entryClient(RestClient.Builder restClientBuilder, EntryProps props) {
		RestClient restClient = restClientBuilder.baseUrl(props.apiUrl())
			.defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(props.clientId(), props.clientSecret()))
			.build();
		RestClientAdapter adapter = RestClientAdapter.create(restClient);
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
		return factory.createClient(EntryClient.class);
	}

}
