package am.ik.note.password;

import am.ik.note.reader.ReaderId;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

@Builder(style = BuilderStyle.STAGED)
public record PasswordResetCompletedEvent(ReaderId readerId) {
}
