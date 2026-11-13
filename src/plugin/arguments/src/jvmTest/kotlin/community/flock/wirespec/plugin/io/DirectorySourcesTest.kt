package community.flock.wirespec.plugin.io

import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldContainExactly
import java.nio.file.Files
import kotlin.test.Test

class DirectorySourcesTest {

    @Test
    fun readsTheWirespecFilesOfAFolderInPathOrder() {
        val names = listOf("orders", "api", "monitoring", "customers", "billing", "zones", "accounts")
        val folder = Files.createTempDirectory("wirespec-directory").toFile().apply {
            deleteOnExit()
            names.forEach { resolve("$it.ws").writeText("type ${it.replaceFirstChar(Char::uppercase)} { id: String }") }
        }

        Directory(DirectoryPath(folder.absolutePath)).wirespecSources(noLogger)
            .shouldBeRight()
            .map { it.name.value } shouldContainExactly names.sorted()
    }
}
