package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.Language.Jvm.Kotlin
import community.flock.wirespec.compiler.cli.Language.Spec.Wirespec
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
                    Options.Output -> "output"
                    Options.Language -> "Wirespec"
                    Options.PackageName -> "packageName"
                    Options.Strict -> null
                    Options.Debug -> null
                }
            )
        }.toTypedArray()
        WirespecCli.run({
            it.input shouldBe "input"
            it.operation.shouldBeTypeOf<Operation.Compile>()
            it.output shouldBe "output"
            it.languages shouldBe setOf(Wirespec)
            it.packageName shouldBe "packageName"
            it.strict shouldBe true
            it.debug shouldBe true
        }, {})(arrayOf("compile", "input") + opts)
    }

    @Test
    fun testMinimumCliArgumentsForCompile() {
        WirespecCli.run({
            it.operation.shouldBeTypeOf<Operation.Compile>()
            it.input shouldBe "input"
            it.output.shouldBeNull()
            it.languages shouldBe setOf(Kotlin)
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
            it.strict shouldBe false
            it.debug shouldBe false
        }, {})(arrayOf("compile", "input", "-l", "Kotlin"))
    }

    @Test
    fun testMinimumCliArgumentsForConvert() {
        WirespecCli.run({ }, {
            it.operation.shouldBeTypeOf<Operation.Convert>()
            it.input shouldBe "input"
            it.output.shouldBeNull()
            it.languages shouldBe setOf(Wirespec)
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
            it.strict shouldBe false
            it.debug shouldBe false
        })(arrayOf("convert", "input", "openapiv2"))
    }

    @Test
    fun testCommandLineEntitiesParser() {
        WirespecCli.run({}, {
            it.operation.shouldBeTypeOf<Operation.Convert>().run {
                format shouldBe Format.OpenApiV2
            }
            it.input shouldBe "input"
            it.output shouldBe "output"
            it.languages shouldBe setOf(Kotlin)
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
            it.strict shouldBe false
            it.debug shouldBe false
        })(arrayOf("convert", "input", "openapiv2", "-o", "output", "-l", "Kotlin"))
    }
}
