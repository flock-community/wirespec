package community.flock.wirespec.integration.spring.kotlin.configuration

import community.flock.wirespec.integration.spring.shared.RawJsonBody
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints

@Configuration
@ImportRuntimeHints(WirespecNativeConfiguration.WirespecLibraryHints::class)
open class WirespecNativeConfiguration {

    class WirespecLibraryHints : RuntimeHintsRegistrar {
        override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
            val allMembers = MemberCategory.entries.toTypedArray()

            hints.reflection().registerType(RawJsonBody::class.java, *allMembers)
            hints.reflection().registerType(Wirespec.RawRequest::class.java, *allMembers)
            hints.reflection().registerType(Wirespec.RawResponse::class.java, *allMembers)

            hints.resources().registerPattern("META-INF/*.kotlin_module")
        }
    }
}
