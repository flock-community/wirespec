package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.Language.Jvm.Kotlin
import community.flock.wirespec.compiler.cli.Language.Spec.Wirespec
import community.flock.wirespec.compiler.cli.io.Extension
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
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
                    Options.Strict -> null
                    Options.Debug -> null
                }
            )
        }.filterNot { it == "-f" }.toTypedArray()
        WirespecCli.provide({
            it.input.shouldBeTypeOf<FullDirPath>().path shouldBe "input"
            it.operation.shouldBeTypeOf<Operation.Compile>()
            it.output shouldBe "output"
            it.languages shouldBe setOf(Wirespec)
            it.packageName shouldBe "packageName"
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
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
            it.strict shouldBe false
            it.debug shouldBe false
        }, {})(arrayOf("compile", "-l", "Kotlin"))
    }

    @Test
    fun testMinimumCliArgumentsForConvert() {
        WirespecCli.provide({ }, {
            it.operation.shouldBeTypeOf<Operation.Convert>()
            it.input.shouldBeTypeOf<FullFilePath>().run {
                fileName shouldBe "swagger"
                extension shouldBe Extension.Json
            }
            it.output.shouldBeNull()
            it.languages shouldBe setOf(Wirespec)
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
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
            it.output shouldBe "output"
            it.languages shouldBe setOf(Kotlin)
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
            it.strict shouldBe false
            it.debug shouldBe false
        })(arrayOf("convert", "openapiv2", "-o", "output", "-l", "Kotlin"))
    }
}
