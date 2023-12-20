package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.test.Test

class CommandLineEntitiesTest {

    @Test
    fun testMinimumCliArguments() {
        CommandLineEntitiesParser(arrayOf("compile", "input"))
            .parse()
            .run {
                command.shouldBeTypeOf<Compile>()
                    .input shouldBe "input"
                output.shouldBeNull()
                languages shouldBe emptySet()
                packageName shouldBe DEFAULT_PACKAGE_NAME
                strict shouldBe false
                debug shouldBe false
            }
    }

    @Test
    fun testCommandLineEntitiesParser() {
        CommandLineEntitiesParser(arrayOf("convert", "input", "open_api_v2", "-o", "output", "-l", "Kotlin"))
            .parse()
            .run {
                command.shouldBeTypeOf<Convert>().run {
                    input shouldBe "input"
                    format shouldBe Format.OPEN_API_V2
                }
                output shouldBe "output"
                languages shouldBe setOf(Language.Jvm.Kotlin)
                packageName shouldBe DEFAULT_PACKAGE_NAME
                strict shouldBe false
                debug shouldBe false
            }
    }
}
