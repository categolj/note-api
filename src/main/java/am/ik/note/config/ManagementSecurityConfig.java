package am.ik.note.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.GET;

@ManagementContextConfiguration(proxyBeanMethods = false)
public class ManagementSecurityConfig {

	@Bean
	@Order(1)
	public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http)
			throws Exception {
		return http.securityMatcher(EndpointRequest.toAnyEndpoint())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll())
				.build();
	}

	@Bean
	@Order(1)
	public SecurityFilterChain additionalManagementSecurityFilterChain(HttpSecurity http)
			throws Exception {
		return http.securityMatcher("/readyz", "/livez").authorizeHttpRequests(
				auth -> auth.requestMatchers(GET, "/readyz", "/livez").permitAll())
				.build();
	}
}
