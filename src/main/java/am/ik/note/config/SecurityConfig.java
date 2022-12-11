package am.ik.note.config;

import am.ik.note.reader.ReaderMapper;
import am.ik.note.reader.ReaderUserDetailsService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RsaKeyProperties.class)
public class SecurityConfig {
	private final RsaKeyProperties rsaKeys;

	public SecurityConfig(RsaKeyProperties rsaKeys) {
		this.rsaKeys = rsaKeys;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(OPTIONS).permitAll()
						.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
						.requestMatchers("/oauth/token").permitAll()
						.requestMatchers("/password_reset", "/password_reset/**").permitAll()
						.requestMatchers(POST, "/readers/*/activations/*", "/readers").permitAll()
						.requestMatchers(GET, "/notes", "/notes/**").hasAnyAuthority("SCOPE_note:read", "SCOPE_note:admin")
						.requestMatchers(POST, "/notes/*/subscribe").hasAnyAuthority("SCOPE_note:read", "SCOPE_note:admin")
						.requestMatchers(POST, "/notes/**").hasAnyAuthority("SCOPE_note:admin")
						.requestMatchers(PUT, "/notes/**").hasAnyAuthority("SCOPE_note:admin")
						.requestMatchers(DELETE, "/notes").hasAnyAuthority("SCOPE_note:admin")
						.anyRequest().hasAnyAuthority("SCOPE_note:admin")
				)
				.csrf(AbstractHttpConfigurer::disable)
				.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
				.cors(Customizer.withDefaults())
				.sessionManagement(c -> c.sessionCreationPolicy(STATELESS))
				.addFilterAfter(new RequestLoggingFilter(new UriFilter()),
						SecurityContextHolderAwareRequestFilter.class)
				.build();
	}

	@Bean
	public ReaderUserDetailsService readerUserDetailsService(ReaderMapper readerMapper) {
		return new ReaderUserDetailsService(readerMapper);
	}

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		final DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(authenticationProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withPublicKey(this.rsaKeys.publicKey()).build();
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		final JWK jwk = new RSAKey.Builder(this.rsaKeys.publicKey()).privateKey(this.rsaKeys.privateKey()).build();
		final JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}
}
