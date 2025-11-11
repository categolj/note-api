package am.ik.note.config;

import am.ik.note.entry.EntryClient;
import am.ik.note.entry.EntryProps;
import am.ik.spring.http.client.RetryableClientHttpRequestInterceptor;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestTemplateAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EntryProps.class)
public class EntryClientConfig {

	@Bean
	public RestTemplateCustomizer restTemplateCustomizer(EntryProps props,
			LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
		final ExponentialBackOff backOff = new ExponentialBackOff(props.retryInterval().toMillis(), 2);
		backOff.setMaxElapsedTime(props.retryMaxElapsedTime().toMillis());
		return restTemplate -> restTemplate.setInterceptors(
				List.of(logbookClientHttpRequestInterceptor, new RetryableClientHttpRequestInterceptor(backOff)));
	}

	@Bean
	public EntryClient entryClient(RestTemplateBuilder restTemplateBuilder, EntryProps props) {
		final RestTemplate restTemplate = restTemplateBuilder //
			.rootUri(props.apiUrl()) //
			.setConnectTimeout(props.connectTimeout()) //
			.setReadTimeout(props.readTimeout()) //
			.basicAuthentication(props.clientId(), props.clientSecret()) //
			.build();
		final RestTemplateAdapter adapter = RestTemplateAdapter.create(restTemplate);
		final HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
		return factory.createClient(EntryClient.class);
	}

}
