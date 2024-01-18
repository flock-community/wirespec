package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.Format
import community.flock.wirespec.compiler.cli.Language
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MainTest {

    @Test
    fun testFormat() {
        Format.toString() shouldBe "OpenApiV2, OpenApiV3"
    }

    @Test
    fun testLanguages() {
        Language.toString() shouldBe "Java, Kotlin, Scala, TypeScript, Wirespec"
    }
}
