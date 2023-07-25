package am.ik.note.reader;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class ReaderUserDetailsService implements UserDetailsService {
	private final ReaderMapper readerMapper;

	public ReaderUserDetailsService(ReaderMapper readerMapper) {
		this.readerMapper = readerMapper;
	}

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		final Reader reader = (username.contains("@")
				? this.readerMapper.findByEmail(username)
				: this.readerMapper.findById(ReaderId.valueOf(username)))
				.orElseThrow(() -> {
					throw new UsernameNotFoundException(
							"Reader (" + username + ") is not found.");
				});
		return new ReaderUserDetails(reader);
	}
}
