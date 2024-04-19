package community.flock.wirespec.plugin.cli

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FullFilePath
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FullFilePathTest {

    @Test
    fun testParse() {
        FullFilePath.parse("/src/test/resources/test.json").run {
            directory shouldBe "/src/test/resources"
            fileName.value shouldBe "test"
            extension shouldBe FileExtension.Json
        }
    }
}
