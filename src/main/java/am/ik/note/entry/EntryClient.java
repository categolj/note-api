package am.ik.note.entry;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface EntryClient {
	@GetExchange(url = "/entries")
	Entries getEntries();


	@GetExchange(url = "/entries/{entryId}")
	Entry getEntry(@PathVariable("entryId") Long entryId);
}
