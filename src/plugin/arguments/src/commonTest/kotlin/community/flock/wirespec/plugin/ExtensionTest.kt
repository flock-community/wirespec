package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExtensionTest {

    private val packageName = PackageName("community.flock.wirespec.generated")

    @Test
    fun testExtensions() {
        Extension.toString() shouldBe "Avro, GraphQLClient, Jackson, JsonRpc, JvmBuilder, KotestDsl, KotlinxSerialization, RustSerde, SpringMappingAnnotations, SpringNativeHints"
    }

    @Test
    fun testNamesAreTheClassNames() {
        Extension.entries.forEach { extension ->
            val language = FileExtension.entries.first { extension.supports(it) }
            val type = extension.toIrExtension(packageName, language).shouldNotBeNull()::class
            type.simpleName shouldBe "${extension.name}Extension"
            Extension.of(type) shouldBe extension
        }
    }

    @Test
    fun testSupportedLanguages() {
        FileExtension.entries.associateWith { language -> Extension.entries.filter { it.supports(language) } }
            .filterValues { it.isNotEmpty() } shouldBe mapOf(
            FileExtension.Java to listOf(Extension.Avro, Extension.GraphQLClient, Extension.Jackson, Extension.JsonRpc, Extension.JvmBuilder, Extension.SpringMappingAnnotations, Extension.SpringNativeHints),
            FileExtension.Kotlin to Extension.entries - Extension.RustSerde,
            FileExtension.Scala to listOf(Extension.GraphQLClient, Extension.JsonRpc, Extension.JvmBuilder),
            FileExtension.TypeScript to listOf(Extension.JsonRpc),
            FileExtension.Python to listOf(Extension.JsonRpc),
            FileExtension.Rust to listOf(Extension.JsonRpc, Extension.RustSerde),
        )
    }

    @Test
    fun testUnsupportedLanguagesAreLeftOut() {
        Extension.entries.toIrExtensions(packageName, FileExtension.Kotlin) shouldHaveSize Extension.entries.size - 1
        Extension.entries.toIrExtensions(packageName, FileExtension.Wirespec).shouldBeEmpty()
        Extension.Avro.toIrExtension(packageName, FileExtension.TypeScript).shouldBeNull()
    }
}
