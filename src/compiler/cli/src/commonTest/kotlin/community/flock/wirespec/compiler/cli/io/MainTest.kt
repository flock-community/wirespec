package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.Format
import community.flock.wirespec.compiler.cli.Language
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MainTest {

    @Test
    fun testFormat() {
        Format.toString() shouldBe "OPEN_API_V2, OPEN_API_V3"
    }

    @Test
    fun testLanguages() {
        Language.toString() shouldBe "Java, Kotlin, Scala, TypeScript, Wirespec"
    }
}
