package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.plugin.io.FilePath
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FilePathTest {

    @Test
    fun testParse() {
        FilePath("/src/test/resources/test.json").run {
            directory.value shouldBe "/src/test/resources"
            name.value shouldBe "test"
            extension shouldBe FileExtension.JSON
        }
    }
}
