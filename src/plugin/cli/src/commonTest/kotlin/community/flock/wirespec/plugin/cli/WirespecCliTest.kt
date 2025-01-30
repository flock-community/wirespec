package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.cli.io.JavaFile
import community.flock.wirespec.plugin.cli.io.KotlinFile
import community.flock.wirespec.plugin.cli.io.TypeScriptFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class WirespecCliTest {

    private val inputDir = "src/commonTest/resources"
    private fun outputDir() = "../../test/tmp/${getRandomString(8)}"

    @Test
    fun testCliOutput() {
        val packageDir = DEFAULT_GENERATED_PACKAGE_STRING.replace(".", "/")
        val input = "${inputDir}/wirespec"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert, ::write)(arrayOf("compile", "-d", input, "-o", output, "-l", "Kotlin"))

        val file = KotlinFile(FullFilePath("$output/$packageDir", FileName("Type"))).read()

        val expected = """
            |package community.flock.wirespec.generated
            |
            |data class Bla(
            |  val yolo: String,
            |  val `class`: Boolean
            |)
            |
        """.trimMargin()
        file shouldBe expected
    }

    @Test
    fun testCliJavaPackage() {
        val packageName = "community.flock.next"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/wirespec"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert, ::write)(
            arrayOf(
                "compile",
                "-d", input,
                "-o", output,
                "-l", "Java",
                "-p", "community.flock.next",
            )
        )

        val file = JavaFile(FullFilePath("$output/$packageDir", FileName("Bla"))).read()

        val expected = """
            |package community.flock.next;
            |
            |public record Bla (
            |  String yolo,
            |  Boolean _class
            |) {
            |};
            |
        """.trimMargin()
        file shouldBe expected
    }

    @Test
    fun testCliOpenApiPetstoreKotlin() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/openapi/petstore.json"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert, ::write)(
            arrayOf(
                "convert", "openapiv2",
                "-f", input,
                "-o", output,
                "-l", "Kotlin",
                "-p", "community.flock.openapi",
            )
        )

        val path = FullFilePath("$output/$packageDir", FileName("Petstore"))
        val file = KotlinFile(path).read()

        val expected = """
            |data class Pet(
            |  val id: Long?,
            |  val category: Category?,
            |  val name: String,
            |  val photoUrls: List<String>,
            |  val tags: List<Tag?>,
            |  val status: PetStatus?
            |)
            """.trimMargin()

        file shouldContain expected
    }

    @Test
    fun testCliKetoKotlin() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/openapi/keto.json"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert, ::write)(
            arrayOf(
                "convert", "openapiv3",
                "-f", input,
                "-o", output,
                "-l", "Kotlin",
                "-p", "community.flock.openapi",
            )
        )

        val path = FullFilePath("$output/$packageDir", FileName("Keto"))
        val file = KotlinFile(path).read()

        val expected = """
            |data class Relationship(
            |  val namespace: String,
            |  val `object`: String,
            |  val relation: String,
            |  val subject_id: String?,
            |  val subject_set: SubjectSet?
            |)
            """.trimMargin()

        file shouldContain expected
    }

    @Test
    fun testCliOpenapiTypesScript() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "${inputDir}/openapi/petstore.json"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert, ::write)(
            arrayOf(
                "convert", "openapiv2",
                "-f", input,
                "-o", output,
                "-l", "TypeScript",
                "-p", "community.flock.openapi",
            )
        )

        val path = FullFilePath("$output/$packageDir", FileName("Petstore"))
        val file = TypeScriptFile(path).read()

        val expected = """
            |export type Pet = {
            |  "id"?: number,
            |  "category"?: Category,
            |  "name": string,
            |  "photoUrls": string[],
            |  "tags"?: Tag[],
            |  "status"?: PetStatus
            |}
        """.trimMargin()

        file shouldContain expected
    }

    private fun getRandomString(length: Int) = (1..length)
        .map { allowedChars.random() }
        .joinToString("")

    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

}
