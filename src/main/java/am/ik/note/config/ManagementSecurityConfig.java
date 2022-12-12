package am.ik.note.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.GET;

@ManagementContextConfiguration(proxyBeanMethods = false)
public class ManagementSecurityConfig {

	@Bean
	public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
						.requestMatchers(GET, "/readyz", "/livez").permitAll()
				)
				.build();
	}
}
