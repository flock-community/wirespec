package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExtensionTest {

    private val packageName = PackageName("community.flock.wirespec.generated")

    @Test
    fun testExtensions() {
        Extension.toString() shouldBe "Avro, Builder, Jackson, KotestDsl, KotlinxSerialization, SpringMappingAnnotations, SpringNativeHints"
    }

    @Test
    fun testNamesAreTheClassNames() {
        Extension.entries.forEach { extension ->
            listOf(extension).toIrExtensions(packageName, FileExtension.Kotlin).single()::class.simpleName shouldBe "${extension.name}Extension"
        }
    }

    @Test
    fun testSupportedLanguages() {
        FileExtension.entries.associateWith { language -> Extension.entries.filter { it.supports(language) } }
            .filterValues { it.isNotEmpty() } shouldBe mapOf(
            FileExtension.Java to listOf(Extension.Avro, Extension.Builder, Extension.Jackson, Extension.SpringMappingAnnotations, Extension.SpringNativeHints),
            FileExtension.Kotlin to Extension.entries,
            FileExtension.Scala to listOf(Extension.Builder),
        )
    }

    @Test
    fun testUnsupportedLanguagesAreLeftOut() {
        Extension.entries.toIrExtensions(packageName, FileExtension.Kotlin) shouldHaveSize Extension.entries.size
        Extension.entries.toIrExtensions(packageName, FileExtension.TypeScript).shouldBeEmpty()
    }
}
