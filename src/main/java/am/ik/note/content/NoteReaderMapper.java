package am.ik.note.content;

import am.ik.note.reader.ReaderId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NoteReaderMapper {
	private final JdbcClient jdbcClient;

	public NoteReaderMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcClient = JdbcClient.create(jdbcTemplate);
	}

	public int countByNoteIdAndReaderId(NoteId noteId, ReaderId readerId) {
		final Long count = this.jdbcClient.sql("""
				SELECT COUNT(*) AS count
				FROM note_reader AS nr
				WHERE nr.note_id = ?
				  AND nr.reader_id = ?
				""").param(noteId.toString()) //
				.param(readerId.toString()) //
				.query((rs, rowNum) -> rs.getLong("count")) //
				.single();
		return count.intValue();
	}

	@Transactional
	public int insertNoteReader(NoteId noteId, ReaderId readerId) {
		return this.jdbcClient.sql("""
				INSERT INTO note_reader(note_id, reader_id) VALUES (?, ?)
				""") //
				.param(noteId.toString()) //
				.param(readerId.toString()) //
				.update();
	}

}
