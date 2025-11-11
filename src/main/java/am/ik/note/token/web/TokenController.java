package am.ik.note.token.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import am.ik.note.reader.ReaderUserDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import static java.util.stream.Collectors.toCollection;

@RestController
@Tag(name = "token")
public class TokenController {

	private final AuthenticationManager authenticationManager;

	private final JwtEncoder jwtEncoder;

	private final Clock clock;

	private final Logger log = LoggerFactory.getLogger(TokenController.class);

	public TokenController(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, Clock clock) {
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.clock = clock;
	}

	@PostMapping(path = "oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ApiResponses({
			@ApiResponse(responseCode = "200",
					content = @Content(schema = @Schema(implementation = OAuth2Token.class))),
			@ApiResponse(responseCode = "401",
					content = @Content(schema = @Schema(implementation = OAuth2Error.class))) })
	public ResponseEntity<?> token(TokenInput input, UriComponentsBuilder builder) {
		final Authentication authentication = new UsernamePasswordAuthenticationToken(input.username(),
				input.password());
		try {
			final Authentication authenticated = this.authenticationManager.authenticate(authentication);
			final UserDetails userDetails = (UserDetails) authenticated.getPrincipal();
			final String issuer = builder.path("oauth/token").build().toString();
			final String subject = userDetails instanceof ReaderUserDetails
					? ((ReaderUserDetails) userDetails).getReader().readerId().toString() : userDetails.getUsername();
			final Instant issuedAt = Instant.now(this.clock);
			final Instant expiresAt = issuedAt.plus(12, ChronoUnit.HOURS);
			final Set<String> scope = userDetails.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(toCollection(TreeSet::new));
			log.atInfo()
				.addKeyValue("username", userDetails.getUsername())
				.addKeyValue("scope", scope)
				.addKeyValue("expires_at", expiresAt)
				.log("""
						msg="The token was issued successfully"
						""".trim());
			final JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(subject)
				.audience(List.of("note.ik.am"))
				.claim("scope", scope)
				.claim("preferred_username", userDetails.getUsername())
				.build();
			final Jwt jwt = this.jwtEncoder.encode(JwtEncoderParameters.from(claims));
			return ResponseEntity.ok(new OAuth2Token(jwt.getTokenValue(), TokenType.BEARER.getValue(),
					Duration.between(issuedAt, expiresAt).getSeconds(), scope));
		}
		catch (AuthenticationException e) {
			log.atWarn().addKeyValue("username", input.username()).addKeyValue("reason", e.getMessage()).log("""
					msg="Authentication failed"
					""".trim());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new OAuth2Error("unauthorized", e.getMessage()));
		}
	}

	public record TokenInput(String username, String password) {
	}

	public record OAuth2Token(@NonNull @JsonProperty("access_token") String accessToken,
			@NonNull @JsonProperty("token_type") String tokenType, @NonNull @JsonProperty("expires_in") long expiresIn,
			@NonNull Set<String> scope) {

	}

	public record OAuth2Error(@NonNull String error,
			@NonNull @JsonProperty("error_description") String errorDescription) {

	}

}
