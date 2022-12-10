package am.ik.note.reader;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static am.ik.note.utils.JdbcTemplateWrapper.wrapQuery;

@Repository
public class ReaderMapper {
	private final JdbcTemplate jdbcTemplate;

	private final RowMapper<Reader> readerRowMapper = (rs, i) -> {
		final String email = rs.getString("email");
		final String hashedPassword = rs.getString("hashed_password");
		final ReaderId id = ReaderId.valueOf(rs.getString("reader_id"));
		final String readerState = rs.getString("reader_state");
		return new Reader(id, email, hashedPassword, ReaderState.valueOf(readerState.toUpperCase()));
	};

	public ReaderMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Reader> findById(ReaderId readerId) {
		return wrapQuery(() -> this.jdbcTemplate.queryForObject("""
				SELECT r.reader_id, r.email, rp.hashed_password, r.reader_state, r.created_at 
				FROM reader AS r 
					LEFT JOIN reader_password rp on r.reader_id = rp.reader_id 
				WHERE r.reader_id = ?
				""", this.readerRowMapper, readerId.toString()));
	}

	public Optional<Reader> findByEmail(String email) {
		return wrapQuery(() -> this.jdbcTemplate.queryForObject("""
				SELECT r.reader_id, r.email, rp.hashed_password, r.reader_state, r.created_at 
				FROM reader AS r 
					LEFT JOIN reader_password rp on r.reader_id = rp.reader_id 
				WHERE r.email = ?
				""", this.readerRowMapper, email));
	}

	public List<Reader> findAll() {
		return this.jdbcTemplate.query("""
				SELECT reader_id, email, '' AS hashed_password, reader_state, created_at 
				FROM reader 
				ORDER BY created_at DESC
				""", this.readerRowMapper);
	}

	@Transactional
	public int insert(ReaderId readerId, String email) {
		return this.jdbcTemplate.update("""
				INSERT INTO reader(reader_id, email) VALUES(?, ?)
				""", readerId.toString(), email);
	}

	@Transactional
	public int updateReaderState(ReaderId readerId, ReaderState readerState) {
		return this.jdbcTemplate.update("""
				UPDATE reader SET reader_state = ? WHERE reader_id = ?
				""", readerState.name(), readerId.toString());
	}
}