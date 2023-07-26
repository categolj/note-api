package am.ik.note.reader;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class ReaderMapper {
	private final JdbcClient jdbcClient;

	private final RowMapper<Reader> readerRowMapper = (rs, i) -> {
		final String email = rs.getString("email");
		final String hashedPassword = rs.getString("hashed_password");
		final ReaderId id = ReaderId.valueOf(rs.getString("reader_id"));
		final String readerState = rs.getString("reader_state");
		return new Reader(id, email, hashedPassword,
				ReaderState.valueOf(readerState.toUpperCase()));
	};

	public ReaderMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcClient = JdbcClient.create(jdbcTemplate);
	}

	public Optional<Reader> findById(ReaderId readerId) {
		return this.jdbcClient
				.sql("""
						SELECT r.reader_id, r.email, rp.hashed_password, r.reader_state, r.created_at
						FROM reader AS r
							LEFT JOIN reader_password rp on r.reader_id = rp.reader_id
						WHERE r.reader_id = ?
						""") //
				.param(readerId.toString()) //
				.query(this.readerRowMapper) //
				.optional();
	}

	public Optional<Reader> findByEmail(String email) {
		return this.jdbcClient
				.sql("""
						SELECT r.reader_id, r.email, rp.hashed_password, r.reader_state, r.created_at
						FROM reader AS r
							LEFT JOIN reader_password rp on r.reader_id = rp.reader_id
						WHERE r.email = ?
						""") //
				.param(email) //
				.query(this.readerRowMapper) //
				.optional();
	}

	public List<Reader> findAll() {
		return this.jdbcClient.sql("""
				SELECT reader_id, email, '' AS hashed_password, reader_state, created_at
				FROM reader
				ORDER BY created_at DESC
				""") //
				.query(this.readerRowMapper) //
				.list();
	}

	@Transactional
	public int insert(ReaderId readerId, String email) {
		return this.jdbcClient.sql("""
				INSERT INTO reader(reader_id, email) VALUES(?, ?)
				""") //
				.param(readerId.toString()) //
				.param(email) //
				.update();
	}

	@Transactional
	public int updateReaderState(ReaderId readerId, ReaderState readerState) {
		return this.jdbcClient.sql("""
				UPDATE reader SET reader_state = ? WHERE reader_id = ?
				""") //
				.param(readerState.name()) //
				.param(readerId.toString()) //
				.update();
	}

	@Transactional
	public int deleteByEmail(String email) {
		return this.jdbcClient.sql("""
				DELETE FROM reader WHERE email = ?
				""") //
				.param(email) //
				.update();
	}
}