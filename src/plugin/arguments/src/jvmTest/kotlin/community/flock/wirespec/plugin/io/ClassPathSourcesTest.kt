package community.flock.wirespec.plugin.io

import community.flock.wirespec.plugin.io.Source.Type.JSON
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test

class ClassPathSourcesTest {

    private val files = mapOf(
        "com/example/api/config/First.ws" to "endpoint First GET /first -> { 200 -> String }",
        "com/example/api/config/nested/Second.ws" to "endpoint Second GET /second -> { 200 -> String }",
        "com/example/api/other/Ignored.ws" to "endpoint Ignored GET /ignored -> { 200 -> String }",
        "openapi/petstore.json" to """{ "openapi": "3.0.0" }""",
    )

    // A jar without directory entries, as some build tools write them.
    private fun jar(): File = Files.createTempFile("wirespec-classpath", ".jar").toFile().apply {
        deleteOnExit()
        JarOutputStream(outputStream()).use { output ->
            files.forEach { (name, content) ->
                output.putNextEntry(JarEntry(name))
                output.write(content.toByteArray())
                output.closeEntry()
            }
        }
    }

    private fun directory(): File = Files.createTempDirectory("wirespec-classpath").toFile().apply {
        deleteOnExit()
        files.forEach { (name, content) -> resolve(name).apply { parentFile.mkdirs() }.writeText(content) }
    }

    private fun List<Source<*>>.names() = map { it.name.value }.sorted()

    @Test
    fun readsTheWirespecFilesBeneathAFolderInAJar() {
        listOf("com/example/api/config", "com.example.api.config", "/com/example/api/config/").forEach { folder ->
            ClassPath(folder).wirespecSources(listOf(jar())).names() shouldContainExactly listOf("First", "Second")
        }
    }

    @Test
    fun readsTheWirespecFilesBeneathAFolderInADirectory() {
        listOf("com/example/api/config", "com.example.api.config").forEach { folder ->
            ClassPath(folder).wirespecSources(listOf(directory())).names() shouldContainExactly listOf("First", "Second")
        }
    }

    @Test
    fun readsAFolderAcrossEntriesWithoutDuplicates() {
        val directory = directory()
        ClassPath("com/example/api/config").wirespecSources(listOf(jar(), directory, directory)).names() shouldContainExactly listOf("First", "Second")
    }

    @Test
    fun readsOneWirespecFile() {
        listOf(jar(), directory()).forEach { entry ->
            ClassPath("com/example/api/config/First.ws").wirespecSources(listOf(entry)) shouldContainExactly
                listOf(Source(Name("First"), files.getValue("com/example/api/config/First.ws")))
        }
    }

    @Test
    fun readsOneFileOfAnyType() {
        listOf(jar(), directory()).forEach { entry ->
            ClassPath("openapi/petstore.json").readSource<JSON>(listOf(entry)) shouldBe
                Source(Name("petstore"), files.getValue("openapi/petstore.json"))
        }
    }

    @Test
    fun findsNothingThatIsNotOnTheClasspath() {
        val classpath = listOf(jar(), directory())
        ClassPath("com/example/missing").wirespecSources(classpath).shouldBeEmpty()
        ClassPath("com/example/api/config/Missing.ws").wirespecSources(classpath).shouldBeEmpty()
        ClassPath("openapi").readSource<JSON>(classpath).shouldBeNull()
        ClassPath("openapi/petstore.json").readSource<JSON>(emptyList()).shouldBeNull()
    }
}
