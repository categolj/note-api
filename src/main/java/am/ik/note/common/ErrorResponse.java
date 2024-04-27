package am.ik.note.common;

import org.springframework.lang.NonNull;

public record ErrorResponse(@NonNull String message, @NonNull String noteUrl) {
}
