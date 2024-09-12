package community.flock.wirespec.plugin

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LanguageTest {
    @Test
    fun testLanguages() {
        Language.toString() shouldBe  "Java, JavaLegacy, Kotlin, KotlinLegacy, Scala, TypeScript, Wirespec"
    }
}
