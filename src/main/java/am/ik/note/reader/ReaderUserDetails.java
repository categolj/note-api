package am.ik.note.reader;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

public class ReaderUserDetails implements UserDetails {
	private final Reader reader;

	public ReaderUserDetails(Reader reader) {
		this.reader = reader;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.createAuthorityList("note:read", "openid");
	}

	@Override
	public String getPassword() {
		return this.reader.hashedPassword();
	}

	@Override
	public String getUsername() {
		return this.reader.email();
	}

	public Reader getReader() {
		return this.reader;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !this.reader.isLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return !this.reader.isDisabled();
	}
}