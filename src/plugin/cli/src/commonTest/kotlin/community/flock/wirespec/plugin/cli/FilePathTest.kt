package community.flock.wirespec.plugin.cli

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.files.FilePath
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FilePathTest {

    @Test
    fun testParse() {
        FilePath("/src/test/resources/test.json").run {
            directory.value shouldBe "/src/test/resources"
            fileName.value shouldBe "test"
            extension shouldBe FileExtension.JSON
        }
    }
}
