package am.ik.note.content;

import am.ik.note.entry.Entry;
import am.ik.note.entry.EntryClient;
import am.ik.note.reader.ReaderId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toUnmodifiableMap;

@Service
public class NoteService {

	private final NoteMapper noteMapper;

	private final NoteReaderMapper noteReaderMapper;

	private final EntryClient entryClient;

	public NoteService(NoteMapper noteMapper, NoteReaderMapper noteReaderMapper, EntryClient entryClient) {
		this.noteMapper = noteMapper;
		this.noteReaderMapper = noteReaderMapper;
		this.entryClient = entryClient;
	}

	@Transactional
	public List<NoteSummary> findAll(ReaderId readerId) {
		final List<NoteSummaryBuilder> summaries = this.noteMapper.findAll(readerId);
		if (summaries.isEmpty()) {
			return List.of();
		}
		final Map<Long, Entry> entryMap = this.entryClient.getEntries()
			.content()
			.stream()
			.collect(toUnmodifiableMap(Entry::entryId, Function.identity()));
		return summaries.stream().map(b -> {
			final Entry entry = entryMap.get(Objects.requireNonNull(b.getEntryId()));
			if (entry == null) {
				return b.build();
			}
			return b.withTitle(entry.frontMatter().title()).withUpdatedDate(entry.updated().date()).build();
		}).toList();
	}

	@Transactional(readOnly = true)
	public Optional<NoteDetails> findByEntryId(Long entryId, ReaderId readerId) {
		return this.noteMapper.findByEntryId(entryId).map(note -> this.toDetails(note, readerId));
	}

	@Transactional(readOnly = true)
	public Optional<NoteDetails> findByNoteId(NoteId noteId, ReaderId readerId) {
		return this.noteMapper.findByNoteId(noteId).map(note -> this.toDetails(note, readerId));
	}

	private NoteDetails toDetails(Note note, ReaderId readerId) {
		int count = this.noteReaderMapper.countByNoteIdAndReaderId(note.noteId(), readerId);
		if (count <= 0) {
			throw new NoteNotSubscribedException("You are not allowed to access to the entry.", note.noteUrl());
		}
		final Entry entry = this.entryClient.getEntry(note.entryId());
		return new NoteDetails(note.noteId(), note.entryId(), entry.content(), entry.frontMatter(), note.noteUrl(),
				entry.created(), entry.updated());
	}

	@Transactional
	public SubscriptionStatus subscribe(NoteId noteId, ReaderId readerId) {
		final int count = this.noteReaderMapper.countByNoteIdAndReaderId(noteId, readerId);
		if (count > 0) {
			return SubscriptionStatus.EXISTING;
		}
		this.noteReaderMapper.insertNoteReader(noteId, readerId);
		return SubscriptionStatus.NEW;

	}

	public enum SubscriptionStatus {

		EXISTING, NEW;

		public boolean isAlreadySubscribed() {
			return this == EXISTING;
		}

	}

}
