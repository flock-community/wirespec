package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.io.Extension
import community.flock.wirespec.compiler.cli.io.File
import community.flock.wirespec.compiler.cli.io.FullFilePath
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals


class CliTest {

    fun outputDir() = "/tmp/${getRandomString(8)}"
    val inputDir = "src/commonTest/resources"

    class TestFile(val directory: String, val fileName: String, val extension: Extension) : File(FullFilePath(directory, fileName, extension)) {
        override fun copy(fileName: String) = TestFile(directory, fileName, extension)
    }

    @Test
    @Ignore
    fun testCliHelp() {
        cli(arrayOf("--help"))
    }

    @Test
    fun testCliOutput() {
        val packageDir = DEFAULT_PACKAGE_NAME.replace(".", "/")
        val outputDir = outputDir()
        cli(arrayOf(inputDir, "-o", outputDir))

        val kotlin = TestFile("$outputDir/$packageDir", "Bla", Extension.Kotlin).read()

        val expected = """
            package community.flock.wirespec.generated

            data class Bla(
              val yolo: String
            )
            
        """.trimIndent()
        assertEquals(expected, kotlin)
    }

    @Test
    fun testCliJavaPackage() {
        val packageName = "community.flock.next"
        val packageDir = packageName.replace(".", "/")
        val outputDir = outputDir()
        cli(arrayOf(inputDir, "-o", outputDir, "-l", "Kotlin", "-l", "Java", "-p", "community.flock.next"))

        val java = TestFile("$outputDir/$packageDir", "Bla", Extension.Java).read()

        val expected = """
            package community.flock.next;

            public record Bla(
              String yolo
            ) {};
            
            
        """.trimIndent()
        assertEquals(expected, java)
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}