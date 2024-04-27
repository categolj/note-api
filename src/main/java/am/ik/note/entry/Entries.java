package am.ik.note.entry;

import java.util.List;

import org.springframework.lang.NonNull;

public record Entries(@NonNull List<Entry> content) {
}
