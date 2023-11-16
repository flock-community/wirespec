package community.flock.wirespec.compiler.cli.io

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FullFilePathTest {

    @Test
    fun testParse() {
        FullFilePath.parse("/src/test/resources/test.json").run {
            directory shouldBe "/src/test/resources"
            fileName shouldBe "test"
            extension shouldBe Extension.Json
        }
    }
}
