package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
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
