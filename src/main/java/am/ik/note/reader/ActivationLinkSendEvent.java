package am.ik.note.reader;

import java.net.URI;
import java.time.OffsetDateTime;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

@Builder(style = BuilderStyle.STAGED)
public record ActivationLinkSendEvent(String email, URI link, OffsetDateTime expiry) {
}
