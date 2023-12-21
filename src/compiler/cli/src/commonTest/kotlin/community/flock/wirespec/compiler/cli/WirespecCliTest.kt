package community.flock.wirespec.compiler.cli

import com.github.ajalt.clikt.core.subcommands
import community.flock.wirespec.compiler.cli.io.FullFilePath
import community.flock.wirespec.compiler.cli.io.JavaFile
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertTrue

class WirespecCliTest {

    private val inputDir = "src/commonTest/resources"
    private fun outputDir() = "../../test/tmp/${getRandomString(8)}"

    @Test
    fun testCliOutput() {
        val packageDir = DEFAULT_PACKAGE_NAME.replace(".", "/")
        val input = "${inputDir}/wirespec"
        val output = outputDir()

        WirespecCli().subcommands(
            Compile(::compile),
            Convert(::convert)
        ).main(arrayOf("compile", input, "-o", output, "-l", "Kotlin"))

        val file = KotlinFile(FullFilePath("$output/$packageDir", "Type")).read()

        val expected = """
            package community.flock.wirespec.generated

            data class Bla(
              val yolo: String
            )
            
        """.trimIndent()
        file shouldBe expected
    }

    @Test
    fun testCliJavaPackage() {
        val packageName = "community.flock.next"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/wirespec"
        val output = outputDir()

        WirespecCli().subcommands(
            Compile(::compile),
            Convert(::convert)
        ).main(
            arrayOf(
                "compile", input,
                "-o", output,
                "-l", "Java",
                "-p", "community.flock.next",
            )
        )

        val file = JavaFile(FullFilePath("$output/$packageDir", "Bla")).read()

        val expected = """
            package community.flock.next;

            public record Bla(
              String yolo
            ) {};
            
        """.trimIndent()
        file shouldBe expected
    }

    @Test
    fun testCliOpenApiPetstoreKotlin() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/openapi/petstore.json"
        val output = outputDir()

        WirespecCli().subcommands(
            Compile(::compile),
            Convert(::convert)
        ).main(
            arrayOf(
                "convert", input, "open_api_v2",
                "-o", output,
                "-l", "Kotlin",
                "-p", "community.flock.openapi",
            )
        )

        val path = FullFilePath("$output/$packageDir", "Petstore")
        val file = KotlinFile(path).read()

        val expected = """
            data class Pet(
              val id: Int? = null,
              val category: Category? = null,
              val name: String,
              val photoUrls: List<String>,
              val tags: List<Tag>? = null,
              val status: PetStatus? = null
            )
            """.trimIndent()

        file.contains(expected).shouldBeTrue()
    }

    @Test
    fun testCliKetoKotlin() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/openapi/keto.json"
        val output = outputDir()

        WirespecCli().subcommands(
            Compile(::compile),
            Convert(::convert)
        ).main(
            arrayOf(
                "convert", input, "open_api_v3",
                "-o", output,
                "-l", "Kotlin",
                "-p", "community.flock.openapi",
            )
        )

        val path = FullFilePath("$output/$packageDir", "Keto")
        val file = KotlinFile(path).read()

        val expected = """
            data class Relationship(
              val namespace: String,
              val `object`: String,
              val relation: String,
              val subject_id: String? = null,
              val subject_set: SubjectSet? = null
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

        WirespecCli().subcommands(
            Compile(::compile),
            Convert(::convert)
        ).main(
            arrayOf(
                "convert", input, "open_api_v2",
                "-o", output,
                "-l", "TypeScript",
                "-p", "community.flock.openapi",
            )
        )

        val path = FullFilePath("$output/$packageDir", "Petstore")
        val file = TypeScriptFile(path).read()

        val expected = """
            export type Pet = {
              "id"?: number,
              "category"?: Category,
              "name": string,
              "photoUrls": string[],
              "tags"?: Tag[],
              "status"?: PetStatus
            }
            """.trimIndent()

        file shouldContain expected
    }

    private fun getRandomString(length: Int) = (1..length)
        .map { allowedChars.random() }
        .joinToString("")

    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

}
