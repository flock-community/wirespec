package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.FullDirPath
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.Operation
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.test.Test

@OptIn(ExperimentalStdlibApi::class)
class CommandLineEntitiesTest {

    @Test
    fun testAllOptions() {
        val opts = Options.entries.flatMap {
            listOfNotNull(
                it.flags.first(), when (it) {
                    Options.InputFile -> null
                    Options.InputDir -> "input"
                    Options.OutputDir -> "output"
                    Options.Language -> "Wirespec"
                    Options.PackageName -> "packageName"
                    // To enable flags they only need the flag name. Therefore, the 'argument' part is null.
                    Options.Shared -> null
                    Options.Strict -> null
                    Options.Debug -> null
                }
            )
        }.filterNot { it == "-f" }.toTypedArray()
        WirespecCli.provide({
            it.input.shouldBeTypeOf<FullDirPath>().path shouldBe "input"
            it.operation.shouldBeTypeOf<Operation.Compile>()
            it.output?.value shouldBe "output"
            it.languages shouldBe setOf(Wirespec)
            it.packageName.value shouldBe "packageName"
            it.shared.also(::println) shouldBe true
            it.strict shouldBe true
            it.debug shouldBe true
        }, {})(arrayOf("compile") + opts)
    }

    @Test
    fun testMinimumCliArgumentsForCompile() {
        WirespecCli.provide({
            it.operation.shouldBeTypeOf<Operation.Compile>()
            it.input.shouldBeTypeOf<Console>()
            it.output.shouldBeNull()
            it.languages shouldBe setOf(Kotlin)
            it.packageName.value shouldBe DEFAULT_PACKAGE_STRING
            it.shared shouldBe false
            it.strict shouldBe false
            it.debug shouldBe false
        }, {})(arrayOf("compile", "-l", "Kotlin"))
    }

    @Test
    fun testMinimumCliArgumentsForConvert() {
        WirespecCli.provide({ }, {
            it.operation.shouldBeTypeOf<Operation.Convert>()
            it.input.shouldBeTypeOf<FullFilePath>().run {
                fileName.value shouldBe "swagger"
                extension shouldBe FileExtension.Json
            }
            it.output.shouldBeNull()
            it.languages shouldBe setOf(Wirespec)
            it.packageName.value shouldBe DEFAULT_PACKAGE_STRING
            it.shared shouldBe false
            it.strict shouldBe false
            it.debug shouldBe false
        })(arrayOf("convert", "-f", "swagger.json", "openapiv2"))
    }

    @Test
    fun testCommandLineEntitiesParser() {
        WirespecCli.provide({}, {
            it.operation.shouldBeTypeOf<Operation.Convert>().run {
                format shouldBe Format.OpenApiV2
            }
            it.input.shouldBeTypeOf<Console>()
            it.output?.value shouldBe "output"
            it.languages shouldBe setOf(Kotlin)
            it.packageName.value shouldBe DEFAULT_PACKAGE_STRING
            it.shared shouldBe false
            it.strict shouldBe false
            it.debug shouldBe false
        })(arrayOf("convert", "openapiv2", "-o", "output", "-l", "Kotlin"))
    }
}
