package community.flock.wirespec.compiler.cli

import com.github.ajalt.clikt.core.subcommands
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
            listOf(
                it.flag, when (it) {
                    Options.Output -> "output"
                    Options.Language -> "Wirespec"
                    Options.PackageName -> "packageName"
                    Options.Strict -> "true"
                    Options.Debug -> "true"
                }
            )
        }.toTypedArray()
        WirespecCli().subcommands(Compile {
            it.input shouldBe "input"
            it.operation.shouldBeTypeOf<Operation.Compile>()
            it.output shouldBe "output"
            it.languages shouldBe setOf(Wirespec)
            it.packageName shouldBe "packageName"
            it.strict shouldBe true
            it.debug shouldBe true
        }).main(arrayOf("compile", "input") + opts)
    }

    @Test
    fun testMinimumCliArguments() {
        WirespecCli().subcommands(Compile {
            it.operation.shouldBeTypeOf<Operation.Compile>()
            it.input shouldBe "input"
            it.output.shouldBeNull()
            it.languages shouldBe emptySet()
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
            it.strict shouldBe false
            it.debug shouldBe false
        }).main(arrayOf("compile", "input"))
    }

    @Test
    fun testCommandLineEntitiesParser() {
        WirespecCli().subcommands(Convert {
            it.operation.shouldBeTypeOf<Operation.Convert>().run {
                format shouldBe Format.OPEN_API_V2
            }
            it.input shouldBe "input"
            it.output shouldBe "output"
            it.languages shouldBe setOf(Language.Jvm.Kotlin)
            it.packageName shouldBe DEFAULT_PACKAGE_NAME
            it.strict shouldBe false
            it.debug shouldBe false
        }).main(arrayOf("convert", "input", "open_api_v2", "-o", "output", "-l", "Kotlin"))
    }
}
