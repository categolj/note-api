package am.ik.note.config;

import io.micrometer.core.instrument.config.MeterFilter;
import java.util.function.Predicate;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MicrometerConfig {

	@Bean
	public MeterRegistryCustomizer<?> meterRegistryCustomizer() {
		final Predicate<String> negate = new UriFilter().negate();
		return registry -> registry.config() //
			.meterFilter(MeterFilter.deny(id -> {
				final String uri = id.getTag("uri");
				return negate.test(uri);
			}));
	}

}
