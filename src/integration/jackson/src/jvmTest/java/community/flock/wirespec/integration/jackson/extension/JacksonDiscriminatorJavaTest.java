package community.flock.wirespec.integration.jackson.extension;

import community.flock.wirespec.integration.jackson.java.generated.model.Animal;
import community.flock.wirespec.integration.jackson.java.generated.model.Bird;
import community.flock.wirespec.integration.jackson.java.generated.model.Cat;
import community.flock.wirespec.integration.jackson.java.generated.model.Owner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/** The Java models of {@code union.ws}, read and written by their Jackson annotations alone, without the Wirespec module. */
public class JacksonDiscriminatorJavaTest {

    private final com.fasterxml.jackson.databind.ObjectMapper v2 = new com.fasterxml.jackson.databind.ObjectMapper();
    private final tools.jackson.databind.ObjectMapper v3 = tools.jackson.databind.json.JsonMapper.builder().build();

    private final Owner owner = new Owner("Ann", new Cat(9L));
    private final String ownerJson = "{\"name\":\"Ann\",\"animal\":{\"kind\":\"cat\",\"lives\":9}}";
    private final String birdJson = "{\"kind\":\"bird\",\"name\":\"Tweety\"}";
    private final String fishJson = "{\"kind\":\"fish\",\"name\":\"Nemo\"}";

    @Test
    public void jackson2WritesAMemberWithItsOwnFieldAndNothingElse() throws Exception {
        assertEquals(ownerJson, v2.writeValueAsString(owner));
        assertEquals(birdJson, v2.writeValueAsString(new Bird("Tweety")));
    }

    @Test
    public void jackson2ReadsTheMemberItsDiscriminatorNames() throws Exception {
        assertEquals(owner, v2.readValue(ownerJson, Owner.class));
        assertEquals(new Bird("Tweety"), v2.readValue(birdJson, Animal.class));
    }

    @Test
    public void jackson2RejectsALiteralNoMemberHas() {
        assertThrows(Exception.class, () -> v2.readValue(fishJson, Animal.class));
    }

    @Test
    public void jackson3WritesAMemberWithItsOwnFieldAndNothingElse() {
        assertEquals(ownerJson, v3.writeValueAsString(owner));
        assertEquals(birdJson, v3.writeValueAsString(new Bird("Tweety")));
    }

    @Test
    public void jackson3ReadsTheMemberItsDiscriminatorNames() {
        assertEquals(owner, v3.readValue(ownerJson, Owner.class));
        assertEquals(new Bird("Tweety"), v3.readValue(birdJson, Animal.class));
    }

    @Test
    public void jackson3RejectsALiteralNoMemberHas() {
        assertThrows(Exception.class, () -> v3.readValue(fishJson, Animal.class));
    }
}
