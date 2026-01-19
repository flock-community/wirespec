package community.flock.wirespec.integration.spring.shared;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ControllerTest {

    @Test
    public void shouldHandleAmpersandSeparatedParameters() {
        String query = "tags=Smilodon%20Rex&tags=Dodo%20Bird&tags=Mammoth";

        assertEquals(
                Map.of("tags", List.of("Smilodon Rex", "Dodo Bird", "Mammoth")),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandleCommaSeparatedValues() {
        String query = "tags=Smilodon%20Rex,Dodo%20Bird,Mammoth";

        assertEquals(
                Map.of("tags", List.of("Smilodon Rex", "Dodo Bird", "Mammoth")),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandleMixedSeparatorFormat() {
        String query = "tags=Smilodon%20Rex,Dodo%20Bird&tags=Mammoth";

        assertEquals(
                Map.of("tags", List.of("Smilodon Rex", "Dodo Bird", "Mammoth")),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandleMultipleDifferentParameters() {
        String query = "tags=Big%20Cat,Small%20Dog&color=deep%20red&size=very%20large";

        assertEquals(
                Map.of(
                        "tags", List.of("Big Cat", "Small Dog"),
                        "color", List.of("deep red"),
                        "size", List.of("very large")
                ),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandleValuesContainingEqualsSign() {
        String query = "equation=1%2B1%3D2&url=http%3A%2F%2Fexample.com%3Fa%3Db";

        assertEquals(
                Map.of(
                        "equation", List.of("1+1=2"),
                        "url", List.of("http://example.com?a=b")
                ),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandleSpecialCharacters() {
        String query = "text=%21%40%23%24%25%5E%26*%28%29&name=John+Doe";

        assertEquals(
                Map.of(
                        "text", List.of("!@#$%^&*()"),
                        "name", List.of("John Doe")
                ),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandlePlusSignsInQueryParameters() {
        String query = "name=John+Doe&title=Senior+Software+Engineer";

        assertEquals(
                Map.of(
                        "name", List.of("John Doe"),
                        "title", List.of("Senior Software Engineer")
                ),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandleParametersWithEmptyValues() {
        String query = "empty=&next=value";

        assertEquals(
                Map.of(
                        "empty", List.of(""),
                        "next", List.of("value")
                ),
                Controller.extractQueries(query)
        );
    }

    @Test
    public void shouldHandleUnicodeCharacters() {
        String query = "text=%F0%9F%98%8A&name=%E6%97%A5%E6%9C%AC";

        assertEquals(
                Map.of(
                        "text", List.of("😊"),
                        "name", List.of("日本")
                ),
                Controller.extractQueries(query)
        );
    }
}
