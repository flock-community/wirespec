package community.flock.wirespec.plugin.cli

import arrow.core.EitherNel
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.FullDirPath
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.Operation
import community.flock.wirespec.plugin.cli.io.File
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
            noopInput {
                it.input.shouldBeTypeOf<FullDirPath>().path shouldBe "src/commonTest/resources/openapi"
                it.operation.shouldBeTypeOf<Operation.Compile>()
                it.output?.value shouldBe "output"
                it.languages shouldBe setOf(Wirespec)
                it.packageName.value shouldBe "packageName"
                it.logLevel shouldBe ERROR
                it.shared.also(::println) shouldBe true
                it.strict shouldBe true
            },
            noopInput {},
            noopWriter,
        )(arrayOf("compile") + opts)
    }

    @Test
    fun testMinimumCliArgumentsForCompile() {
        WirespecCli.provide(
            noopInput {
                it.operation.shouldBeTypeOf<Operation.Compile>()
                it.input.shouldBeTypeOf<Console>()
                it.output.shouldBeNull()
                it.languages shouldBe setOf(Kotlin)
                it.packageName.value shouldBe DEFAULT_GENERATED_PACKAGE_STRING
                it.logLevel shouldBe ERROR
                it.shared shouldBe false
                it.strict shouldBe false
            },
            noopInput { },
            noopWriter,
        )(arrayOf("compile", "-l", "Kotlin"))
    }

    @Test
    fun testMinimumCliArgumentsForConvert() {
        WirespecCli.provide(
            noopInput { },
            noopInput {
                it.operation.shouldBeTypeOf<Operation.Convert>()
                it.input.shouldBeTypeOf<FullFilePath>().run {
                    fileName.value shouldBe "keto"
                    extension shouldBe FileExtension.Json
                }
                it.output.shouldBeNull()
                it.languages shouldBe setOf(Wirespec)
                it.packageName.value shouldBe DEFAULT_GENERATED_PACKAGE_STRING
                it.logLevel shouldBe ERROR
                it.shared shouldBe false
                it.strict shouldBe false
            },
            noopWriter,
        )(arrayOf("convert", "-i", "src/commonTest/resources/openapi/keto.json", "openapiv2"))
    }

    @Test
    fun testCommandLineEntitiesParser() {
        WirespecCli.provide(
            noopInput { },
            noopInput {
                it.operation.shouldBeTypeOf<Operation.Convert>().run {
                    format shouldBe Format.OpenApiV2
                }
                it.input.shouldBeTypeOf<Console>()
                it.output?.value shouldBe "output"
                it.languages shouldBe setOf(Kotlin)
                it.packageName.value shouldBe DEFAULT_GENERATED_PACKAGE_STRING
                it.logLevel shouldBe ERROR
                it.shared shouldBe false
                it.strict shouldBe false
            },
            noopWriter,
        )(arrayOf("convert", "openapiv2", "-o", "output", "-l", "Kotlin"))
    }

    private fun noopInput(block: (CompilerArguments) -> Unit): (CompilerArguments) -> List<EitherNel<WirespecException, Pair<List<Emitted>, File?>>> = {
        block(it)
        emptyList()
    }

    private val noopWriter: (List<Emitted>, File?) -> Unit = { _, _ -> }
}
