package am.ik.note.reader;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ReaderPasswordMapper {
	private final JdbcTemplate jdbcTemplate;

	public ReaderPasswordMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public int insert(ReaderPassword readerPassword) {
		return this.jdbcTemplate.update("""
				INSERT INTO reader_password(reader_id, hashed_password) VALUES (?, ?)
				""", readerPassword.readerId().toString(),
				readerPassword.hashedPassword());
	}

	@Transactional
	public int deleteByReaderId(ReaderId readerId) {
		return this.jdbcTemplate.update("""
				DELETE FROM reader_password WHERE reader_id = ?
				""", readerId.toString());
	}
}