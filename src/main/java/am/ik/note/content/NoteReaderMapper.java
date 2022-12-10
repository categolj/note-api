package am.ik.note.content;

import am.ik.note.reader.ReaderId;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NoteReaderMapper {
	private final JdbcTemplate jdbcTemplate;

	public NoteReaderMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}


	public int countByNoteIdAndReaderId(NoteId noteId, ReaderId readerId) {
		final Integer count = this.jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM note_reader AS nr
				WHERE nr.note_id = ?
				  AND nr.reader_id = ?
				""", Integer.class, noteId.toString(), readerId.toString());
		return count == null ? -1 : count;
	}

	@Transactional
	public int insertNoteReader(NoteId noteId, ReaderId readerId) {
		return this.jdbcTemplate.update("""
				INSERT INTO note_reader(note_id, reader_id) VALUES (?, ?)
				""", noteId.toString(), readerId.toString());
	}

}