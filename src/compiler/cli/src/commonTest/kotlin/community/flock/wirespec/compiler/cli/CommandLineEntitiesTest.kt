package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CommandLineEntitiesTest {

    @Test
    fun testMinimumCliArguments() {
        CommandLineEntitiesParser(arrayOf("input"))
            .parse()
            .run {
                debug shouldBe false
                input shouldBe "input"
                output.shouldBeNull()
                languages shouldBe emptySet()
                format.shouldBeNull()
                packageName shouldBe DEFAULT_PACKAGE_NAME
                strict shouldBe false
            }
    }

    @Test
    fun testCommandLineEntitiesParser() {
        CommandLineEntitiesParser(arrayOf("input", "-o", "output", "-l", "Kotlin"))
            .parse()
            .run {
                debug shouldBe false
                input shouldBe "input"
                output shouldBe "output"
                languages shouldBe setOf(Language.Jvm.Kotlin)
                format.shouldBeNull()
                packageName shouldBe DEFAULT_PACKAGE_NAME
                strict shouldBe false
            }

    }
}
