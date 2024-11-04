package am.ik.note.password;

import java.net.URI;
import java.time.OffsetDateTime;

import am.ik.note.reader.ReaderId;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

@Builder(style = BuilderStyle.STAGED)
public record PasswordResetLinkSendEvent(ReaderId readerId, URI link, OffsetDateTime expiry) {
}
