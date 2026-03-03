package community.flock.wirespec.integration.spring.java.configuration;

import community.flock.wirespec.integration.spring.shared.RawJsonBody;
import community.flock.wirespec.java.Wirespec;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(WirespecNativeConfiguration.WirespecLibraryHints.class)
public class WirespecNativeConfiguration {

    static class WirespecLibraryHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            var allMembers = MemberCategory.values();

            hints.reflection().registerType(RawJsonBody.class, allMembers);
            hints.reflection().registerType(Wirespec.RawRequest.class, allMembers);
            hints.reflection().registerType(Wirespec.RawResponse.class, allMembers);

            hints.resources().registerPattern("META-INF/*.kotlin_module");
        }
    }
}
