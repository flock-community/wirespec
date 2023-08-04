package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.io.FullFilePath
import community.flock.wirespec.compiler.cli.io.JavaFile
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val input = "${inputDir}/wirespec"
        val output = outputDir()

        cli(arrayOf(input, "-o", output))

        val file = KotlinFile(FullFilePath("$output/$packageDir", "Type")).read()

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
        val input = "${inputDir}/wirespec"
        val output = outputDir()

        cli(arrayOf(input, "-o", output, "-l", "Java", "-p", "community.flock.next"))

        val file = JavaFile(FullFilePath("$output/$packageDir", "Bla")).read()

        val expected = """
            package community.flock.next;

            public record Bla(
              String yolo
            ) {};
            
        """.trimIndent()
        assertEquals(expected, file)
    }

    @Test
    fun testCliOpenapiKotlin() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/openapi/petstore.json"
        val output = outputDir()

        cli(arrayOf(input, "-o", output, "-l", "Kotlin", "-p", "community.flock.openapi", "-a", "v2"))

        val path = FullFilePath("$output/$packageDir", "Petstore")
        val file = KotlinFile(path).read()

        val expected = """
            data class Pet(
              val id: Int? = null,
              val category: Category? = null,
              val name: String,
              val photoUrls: List<String>,
              val tags: List<Tag>? = null,
              val status: String? = null
            )
            """.trimIndent()

        assertTrue(file.contains(expected))
    }

    @Test
    fun testCliOpenapiTypesScript() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/openapi/petstore.json"
        val output = outputDir()

        cli(arrayOf(input, "-o", output, "-l", "TypeScript", "-p", "community.flock.openapi", "-a", "v2"))

        val path = FullFilePath("$output/$packageDir", "Petstore")
        val file = TypeScriptFile(path).read()

        val expected = """
            export type Pet = {
              id?: number,
              category?: Category,
              name: string,
              photoUrls: string[],
              tags?: Tag[],
              status?: string
            }
            """.trimIndent()

        assertTrue(file.contains(expected))
    }

    private fun getRandomString(length: Int) = (1..length)
        .map { allowedChars.random() }
        .joinToString("")

    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

}
