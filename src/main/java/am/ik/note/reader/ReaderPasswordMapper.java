package am.ik.note.reader;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ReaderPasswordMapper {
	private final JdbcClient jdbcClient;

	public ReaderPasswordMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcClient = JdbcClient.create(jdbcTemplate);
	}

	@Transactional
	public int insert(ReaderPassword readerPassword) {
		return this.jdbcClient.sql("""
				INSERT INTO reader_password(reader_id, hashed_password) VALUES (?, ?)
				""") //
				.param(readerPassword.readerId().toString()) //
				.param(readerPassword.hashedPassword()) //
				.update();
	}

	@Transactional
	public int deleteByReaderId(ReaderId readerId) {
		return this.jdbcClient.sql("""
				DELETE FROM reader_password WHERE reader_id = ?
				""") //
				.param(readerId.toString()) //
				.update();
	}
}