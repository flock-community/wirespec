package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.plugin.DirectoryPath
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Format.OpenAPIV2
import community.flock.wirespec.plugin.Language.Wirespec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.test.Test

class CommandLineEntitiesTest {

    @Test
    fun testAllOptions() {
        val opts = Options.entries.flatMap {
            listOfNotNull(
                it.flags.first(),
                when (it) {
                    Options.Input -> "src/commonTest/resources/openapi"
                    Options.Output -> "output"
                    Options.Language -> "Wirespec"
                    Options.PackageName -> "packageName"
                    Options.LogLevel -> "error"
                    // To enable flags they only need the flag name. Therefore, the 'argument' part is null.
                    Options.Shared -> null
                    Options.Strict -> null
                },
            )
        }.toTypedArray()
        WirespecCli.provide(
            noopCompiler {
                it.input.shouldBeTypeOf<DirectoryPath>().value shouldBe "src/commonTest/resources/openapi"
                it.output?.value shouldBe "output"
                it.languages shouldBe setOf(Wirespec)
                it.packageName.value shouldBe "packageName"
                it.logLevel shouldBe ERROR
                it.shared.also(::println) shouldBe true
                it.strict shouldBe true
            },
            noopConverter {},
        ).main(arrayOf("compile") + opts)
    }

    @Test
    fun testMinimumCliArgumentsForConvert() {
        WirespecCli.provide(
            noopCompiler { },
            noopConverter {
                it.format.shouldBeTypeOf<Format>() shouldBe OpenAPIV2
                it.input.shouldBeTypeOf<FilePath>().run {
                    fileName.value shouldBe "keto"
                    extension shouldBe FileExtension.JSON
                }
                it.output.shouldBeNull()
                it.languages shouldBe setOf(Wirespec)
                it.packageName.value shouldBe DEFAULT_GENERATED_PACKAGE_STRING
                it.logLevel shouldBe ERROR
                it.shared shouldBe false
                it.strict shouldBe false
            },
        ).main(arrayOf("convert", "-i", "src/commonTest/resources/openapi/keto.json", "openapiv2"))
    }
}
