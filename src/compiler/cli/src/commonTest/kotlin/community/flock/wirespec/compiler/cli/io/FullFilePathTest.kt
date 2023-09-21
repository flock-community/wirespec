package community.flock.wirespec.compiler.cli.io

import kotlin.test.Ignore
import kotlin.test.Test

class FullFilePathTest {

    @Test
    fun testParse() {
        val result = FullFilePath.parse("/src/test/resources/test.json")
        kotlin.test.assertEquals("/src/test/resources", result.directory)
        kotlin.test.assertEquals("test", result.fileName)
        kotlin.test.assertEquals(Extension.Json, result.extension)
    }
}
