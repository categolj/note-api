package am.ik.note;

import org.junit.jupiter.api.Test;

import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

public class ModularityTest {
	ApplicationModules modules = ApplicationModules.of(NoteApiApplication.class);

	@Test
	void verifiesModularStructure() {
		modules.verify();
	}

	@Test
	void writeDocumentationSnippets() {
		new Documenter(modules).writeModulesAsPlantUml()
				.writeIndividualModulesAsPlantUml();
	}
}
