package am.ik.note.password;

import java.time.ZoneOffset;
import java.util.Optional;

import am.ik.note.reader.ReaderId;
import am.ik.note.utils.JdbcTemplateWrapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PasswordResetMapper {
	private final JdbcTemplate jdbcTemplate;

	public PasswordResetMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<PasswordReset> findByResetId(PasswordResetId resetId) {
		return JdbcTemplateWrapper
				.wrapQuery(() -> this.jdbcTemplate.queryForObject(
						"""
								SELECT reset_id, reader_id, created_at
								FROM password_reset
								WHERE reset_id = ?
								""", (rs,
								i) -> new PasswordReset(resetId,
										ReaderId.valueOf(rs.getString("reader_id")),
										rs.getTimestamp("created_at").toInstant()
												.atOffset(ZoneOffset.UTC)),
						resetId.toString()));
	}

	@Transactional
	public int insert(PasswordResetId resetId, ReaderId readerId) {
		return this.jdbcTemplate.update("""
				INSERT INTO password_reset(reset_id, reader_id) VALUES (?, ?)
				""", resetId.toString(), readerId.toString());
	}

	@Transactional
	public int deleteByResetId(PasswordResetId resetId) {
		return this.jdbcTemplate.update("""
				DELETE FROM password_reset WHERE reset_id = ?
				""", resetId.toString());
	}
}