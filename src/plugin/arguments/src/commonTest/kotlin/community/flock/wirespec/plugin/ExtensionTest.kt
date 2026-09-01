package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExtensionTest {
    @Test
    fun testExtensions() {
        Extension.toString() shouldBe "Avro, Jackson, KotlinxSerialization, SpringMappingAnnotations, SpringNativeHints, KotestDsl"
    }

    @Test
    fun testEveryExtensionCanBeInstantiated() {
        Extension.entries.forEach { extension ->
            extension.toIrExtension(PackageName("community.flock.test"), FileExtension.Kotlin)
        }
    }
}
