package am.ik.note.entry;

import org.springframework.lang.NonNull;

public record Entry(@NonNull Long entryId, @NonNull FrontMatter frontMatter, @NonNull String content,
		@NonNull Author created, @NonNull Author updated) {
}
