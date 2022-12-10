package am.ik.note.utils;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.dao.EmptyResultDataAccessException;

public class JdbcTemplateWrapper {
	public static <T> Optional<T> wrapQuery(Supplier<T> supplier) {
		try {
			T t = supplier.get();
			return Optional.ofNullable(t);
		}
		catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
}