package community.flock.wirespec.plugin

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LanguageTest {
    @Test
    fun testLanguages() {
        Language.toString() shouldBe "Java, Kotlin, Scala, TypeScript, Python, Wirespec, OpenAPIV2, OpenAPIV3"
    }
}
