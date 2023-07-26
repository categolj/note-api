package am.ik.note.content;

import am.ik.note.reader.ReaderId;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class NoteMapper {
	private final JdbcTemplate jdbcTemplate;

	private final RowMapper<Note> noteRowMapper = (rs, i) -> {
		final NoteId noteId = NoteId.valueOf(rs.getString("note_id"));
		final long entryId = rs.getLong("entry_id");
		final String noteUrl = rs.getString("note_url");
		return new Note(noteId, entryId, noteUrl);
	};

	public NoteMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Note> findByNoteId(NoteId noteId) {
		return DataAccessUtils.optionalResult(this.jdbcTemplate.query("""
				SELECT n.note_id, n.entry_id, n.note_url
				FROM note AS n
				WHERE n.note_id = ?
				""", this.noteRowMapper, noteId.toString()));
	}

	public Optional<Note> findByEntryId(Long entryId) {
		return DataAccessUtils.optionalResult(this.jdbcTemplate.query("""
				SELECT n.note_id, n.entry_id, n.note_url
				FROM note AS n
				WHERE n.entry_id = ?
				""", this.noteRowMapper, entryId));
	}

	public List<NoteSummaryBuilder> findAll(ReaderId readerId) {
		return this.jdbcTemplate.query("""
				SELECT n.note_id, n.entry_id, n.note_url, nr.reader_id
				FROM note AS n
				LEFT JOIN note_reader AS nr
					ON n.note_id = nr.note_id
					AND nr.reader_id = ?
				ORDER BY n.entry_id ASC
				""", (rs, i) -> {
			final NoteId noteId = NoteId.valueOf(rs.getString("note_id"));
			final long entryId = rs.getLong("entry_id");
			final String noteUrl = rs.getString("note_url");
			final boolean isSubscribed = rs.getString("reader_id") != null;
			return new NoteSummaryBuilder().withNoteId(noteId).withEntryId(entryId)
					.withNoteUrl(noteUrl).withSubscribed(isSubscribed);
		}, readerId.toString());
	}

	@Transactional
	public int insertNote(NoteId noteId, Long entryId, String noteUrl) {
		return this.jdbcTemplate.update("""
				INSERT INTO note(note_id, entry_id, note_url) VALUES (?, ?, ?)
				""", noteId.toString(), entryId, noteUrl);
	}

	@Transactional
	public int deleteByEntryId(Long entryId) {
		return this.jdbcTemplate.update("""
				DELETE FROM note WHERE entry_id = ?
				""", entryId);
	}
}