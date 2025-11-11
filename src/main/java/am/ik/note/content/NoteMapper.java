package am.ik.note.content;

import am.ik.note.reader.ReaderId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class NoteMapper {

	private final JdbcClient jdbcClient;

	private final RowMapper<Note> noteRowMapper = (rs, i) -> {
		final NoteId noteId = NoteId.valueOf(rs.getString("note_id"));
		final long entryId = rs.getLong("entry_id");
		final String noteUrl = rs.getString("note_url");
		return new Note(noteId, entryId, noteUrl);
	};

	public NoteMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcClient = JdbcClient.create(jdbcTemplate);
	}

	public Optional<Note> findByNoteId(NoteId noteId) {
		return this.jdbcClient.sql("""
				SELECT n.note_id, n.entry_id, n.note_url
				FROM note AS n
				WHERE n.note_id = ?
				""") //
			.param(noteId.toString()) //
			.query(this.noteRowMapper) //
			.optional();
	}

	public Optional<Note> findByEntryId(Long entryId) {
		return this.jdbcClient.sql("""
				SELECT n.note_id, n.entry_id, n.note_url
				FROM note AS n
				WHERE n.entry_id = ?
				""") //
			.param(entryId) //
			.query(this.noteRowMapper) //
			.optional();
	}

	public List<NoteSummaryBuilder> findAll(ReaderId readerId) {
		return this.jdbcClient.sql("""
				SELECT n.note_id, n.entry_id, n.note_url, nr.reader_id
				FROM note AS n
				LEFT JOIN note_reader AS nr
					ON n.note_id = nr.note_id
					AND nr.reader_id = ?
				ORDER BY n.entry_id ASC
				""")
			.param(readerId.toString()) //
			.query((rs, i) -> {
				final NoteId noteId = NoteId.valueOf(rs.getString("note_id"));
				final long entryId = rs.getLong("entry_id");
				final String noteUrl = rs.getString("note_url");
				final boolean isSubscribed = rs.getString("reader_id") != null;
				return new NoteSummaryBuilder().withNoteId(noteId)
					.withEntryId(entryId)
					.withNoteUrl(noteUrl)
					.withSubscribed(isSubscribed);
			}) //
			.list();
	}

	@Transactional
	public int insertNote(NoteId noteId, Long entryId, String noteUrl) {
		return this.jdbcClient.sql("""
				INSERT INTO note(note_id, entry_id, note_url) VALUES (?, ?, ?)
				""") //
			.param(noteId.toString()) //
			.param(entryId) //
			.param(noteUrl) //
			.update();
	}

	@Transactional
	public int deleteByEntryId(Long entryId) {
		return this.jdbcClient.sql("""
				DELETE FROM note WHERE entry_id = ?
				""") //
			.param(entryId) //
			.update();
	}

}