package am.ik.note.entry;

public record Entry(Long entryId, FrontMatter frontMatter, String content, Author created,
					Author updated) {
}
