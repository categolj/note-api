package am.ik.note.token.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import am.ik.note.reader.ReaderUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import static java.util.stream.Collectors.toCollection;

@RestController
public class TokenController {
	private final AuthenticationManager authenticationManager;

	private final JwtEncoder jwtEncoder;

	private final Clock clock;

	private final Logger log = LoggerFactory.getLogger(TokenController.class);

	public TokenController(AuthenticationManager authenticationManager,
			JwtEncoder jwtEncoder, Clock clock) {
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.clock = clock;
	}

	@PostMapping(path = "oauth/token")
	public ResponseEntity<?> token(@RequestParam("username") String username,
			@RequestParam("password") String password, UriComponentsBuilder builder) {
		final Authentication authentication = new UsernamePasswordAuthenticationToken(
				username, password);
		try {
			final Authentication authenticated = this.authenticationManager
					.authenticate(authentication);
			final UserDetails userDetails = (UserDetails) authenticated.getPrincipal();
			final String issuer = builder.path("oauth/token").build().toString();
			final String subject = userDetails instanceof ReaderUserDetails
					? ((ReaderUserDetails) userDetails).getReader().readerId().toString()
					: userDetails.getUsername();
			final Instant issuedAt = Instant.now(this.clock);
			final Instant expiresAt = issuedAt.plus(3, ChronoUnit.HOURS);
			final Set<String> scope = userDetails.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.collect(toCollection(TreeSet::new));
			final JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer)
					.issuedAt(issuedAt).expiresAt(expiresAt).subject(subject)
					.audience(List.of("note.ik.am")).claim("scope", scope)
					.claim("preferred_username", userDetails.getUsername()).build();
			final Jwt jwt = this.jwtEncoder.encode(JwtEncoderParameters.from(claims));
			return ResponseEntity.ok(Map.of("access_token", jwt.getTokenValue(),
					"token_type", TokenType.BEARER.getValue(), "expires_in",
					Duration.between(issuedAt, expiresAt).getSeconds(), "scope", scope));
		}
		catch (AuthenticationException e) {
			log.warn("Authentication failed username:{} message:{}", username,
					e.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
					Map.of("error", "unauthorized", "error_description", e.getMessage()));
		}
	}
}
