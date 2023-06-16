package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.io.FullFilePath
import community.flock.wirespec.compiler.cli.io.JavaFile
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class CliTest {

    private fun outputDir() = "../../test/tmp/${getRandomString(8)}"
    private val inputDir = "src/commonTest/resources"

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

        val file = KotlinFile(FullFilePath("$outputDir/$packageDir", "Bla")).read()

        val expected = """
            package community.flock.wirespec.generated

            data class Bla(
              val yolo: String
            )
            
        """.trimIndent()
        assertEquals(expected, file)
    }

    @Test
    fun testCliJavaPackage() {
        val packageName = "community.flock.next"
        val packageDir = packageName.replace(".", "/")
        val outputDir = outputDir()

        cli(arrayOf(inputDir, "-o", outputDir, "-l", "Kotlin", "-l", "Java", "-p", "community.flock.next"))

        val file = JavaFile(FullFilePath("$outputDir/$packageDir", "Bla")).read()

        val expected = """
            package community.flock.next;

            public record Bla(
              String yolo
            ) {};
            
            
        """.trimIndent()
        assertEquals(expected, file)
    }

    private fun getRandomString(length: Int) = (1..length)
        .map { allowedChars.random() }
        .joinToString("")

    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

}
