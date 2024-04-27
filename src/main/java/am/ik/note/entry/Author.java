package am.ik.note.entry;

import java.time.OffsetDateTime;

import org.springframework.lang.NonNull;

public record Author(@NonNull String name, @NonNull OffsetDateTime date) {
}
