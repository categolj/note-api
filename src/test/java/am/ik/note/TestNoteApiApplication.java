package am.ik.note;

import org.springframework.boot.SpringApplication;

public class TestNoteApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(NoteApiApplication::main)
			.with(TestContainersConfig.class)
			.run("--spring.docker.compose.enabled=false", "--management.tracing.export.zipkin.enabled=false");
	}

}
