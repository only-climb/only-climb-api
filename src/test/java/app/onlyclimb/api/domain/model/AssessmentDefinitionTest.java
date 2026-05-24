package app.onlyclimb.api.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssessmentDefinitionTest {

    @Test
    void translationFieldWhitelistEnforced() {
        UUID testId = UUID.randomUUID();
        AssessmentTest t = new AssessmentTest(testId, "T1", 1, "kg",
                AssessmentValueType.DECIMAL, List.of());

        assertThatThrownBy(() -> new AssessmentDefinition(
                UUID.randomUUID(), "CODE", null, true, List.of(t),
                List.of(new Translation("en", "bogus", "x")),
                Instant.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveField_fallsBackToSpanishThenAny() {
        UUID id = UUID.randomUUID();
        AssessmentDefinition def = new AssessmentDefinition(
                id, "CODE", null, true, List.of(),
                List.of(
                        new Translation("es", "name", "Hola"),
                        new Translation("fr", "name", "Bonjour")),
                Instant.now(), Instant.now());

        assertThat(def.resolveField("name", "fr")).contains("Bonjour");
        assertThat(def.resolveField("name", "en")).contains("Hola");
        assertThat(def.resolveField("name", null)).contains("Hola");
        assertThat(def.resolveField("description", "es")).isEmpty();
    }

    @Test
    void findTestAndTestIds() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        AssessmentTest a = new AssessmentTest(t1, "A", 1, "kg",
                AssessmentValueType.DECIMAL, List.of());
        AssessmentTest b = new AssessmentTest(t2, "B", 2, "s",
                AssessmentValueType.INTEGER, List.of());

        AssessmentDefinition def = new AssessmentDefinition(
                UUID.randomUUID(), "X", ClimbingDiscipline.BOULDER, true,
                List.of(a, b), List.of(), Instant.now(), Instant.now());

        assertThat(def.findTest(t1)).contains(a);
        assertThat(def.findTest(UUID.randomUUID())).isEmpty();
        assertThat(def.testIds()).containsExactly(t1, t2);
        assertThat(def.getTargetDiscipline()).contains(ClimbingDiscipline.BOULDER);
    }
}
