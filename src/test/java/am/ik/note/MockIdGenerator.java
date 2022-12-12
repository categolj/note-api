package am.ik.note;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import org.springframework.util.IdGenerator;

public class MockIdGenerator implements IdGenerator {
	private final Queue<UUID> queue = new LinkedList<>();

	public MockIdGenerator putId(UUID uuid) {
		this.queue.add(uuid);
		return this;
	}

	@Override
	public UUID generateId() {
		return queue.poll();
	}
}
