package am.ik.note.reader.activationlink;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Optional;

import am.ik.note.reader.ReaderId;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static am.ik.note.utils.JdbcTemplateWrapper.wrapQuery;

@Repository
public class ActivationLinkMapper {
	private final JdbcTemplate jdbcTemplate;

	private final RowMapper<ActivationLink> activationLinkRowMapper = (rs, i) -> {
		final ActivationLinkId id = ActivationLinkId.valueOf(rs.getString("activation_id"));
		final ReaderId readerId = ReaderId.valueOf(rs.getString("reader_id"));
		final Timestamp createdAt = rs.getTimestamp("created_at");
		return new ActivationLink(id, readerId, createdAt.toInstant().atOffset(ZoneOffset.UTC));
	};

	public ActivationLinkMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<ActivationLink> findById(ActivationLinkId activationLinkId) {
		return wrapQuery(() -> this.jdbcTemplate.queryForObject("""
				SELECT activation_id, reader_id, created_at FROM activation_link WHERE activation_id = ?
				""", activationLinkRowMapper, activationLinkId.toString()));
	}

	@Transactional
	public int insert(ActivationLink activationLink) {
		return this.jdbcTemplate.update("""
				INSERT INTO activation_link(activation_id, reader_id, created_at) VALUES(?, ?, ?)
				""", activationLink.activationId().toString(), activationLink.readerId().toString(), Timestamp.from(activationLink.createdAt().toInstant()));
	}

	@Transactional
	public int deleteById(ActivationLinkId activationLinkId) {
		return this.jdbcTemplate.update("""
				DELETE FROM activation_link WHERE activation_id = ?
				""", activationLinkId.toString());
	}
}