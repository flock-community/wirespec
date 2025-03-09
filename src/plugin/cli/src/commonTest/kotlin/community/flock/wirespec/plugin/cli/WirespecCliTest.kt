package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.plugin.DirectoryPath
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
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
        val input = "$inputDir/wirespec"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert)
            .main(arrayOf("compile", "-i", input, "-o", output, "-l", "Kotlin"))

        val directoryPath = DirectoryPath("$output/$packageDir")

        KotlinFile(FilePath(directoryPath, FileName("Type"))).read() shouldBe """
            |package community.flock.wirespec.generated
            |
            |data class Bla(
            |  val yolo: String,
            |  val `class`: Boolean
            |)
            |
        """.trimMargin()
    }

    @Test
    fun testCliJavaPackage() {
        val packageName = "community.flock.next"
        val packageDir = packageName.replace(".", "/")
        val input = "$inputDir/wirespec"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert).main(
            arrayOf(
                "compile",
                "-i", input,
                "-o", output,
                "-l", "Java",
                "-p", "community.flock.next",
            ),
        )

        val directoryPath = DirectoryPath("$output/$packageDir")

        JavaFile(FilePath(directoryPath, FileName("Bla"))).read() shouldBe """
            |package community.flock.next;
            |
            |public record Bla (
            |  String yolo,
            |  Boolean _class
            |) {
            |};
            |
        """.trimMargin()
    }

    @Test
    fun testCliOpenAPIPetstoreKotlin() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "$inputDir/openapi/petstore.json"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert).main(
            arrayOf(
                "convert", "openapiv2",
                "-i", input,
                "-o", output,
                "-l", "Kotlin",
                "-p", "community.flock.openapi",
            ),
        )

        val directoryPath = DirectoryPath("$output/$packageDir")
        val path = FilePath(directoryPath, FileName("Petstore"))

        KotlinFile(path).read() shouldContain """
            |data class Pet(
            |  val id: Long?,
            |  val category: Category?,
            |  val name: String,
            |  val photoUrls: List<String>,
            |  val tags: List<Tag>?,
            |  val status: PetStatus?
            |)
        """.trimMargin()
    }

    @Test
    fun testCliKetoKotlin() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "$inputDir/openapi/keto.json"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert).main(
            arrayOf(
                "convert", "openapiv3",
                "-i", input,
                "-o", output,
                "-l", "Kotlin",
                "-p", "community.flock.openapi",
            ),
        )

        val directoryPath = DirectoryPath("$output/$packageDir")
        val path = FilePath(directoryPath, FileName("Keto"))

        KotlinFile(path).read() shouldContain """
            |data class Relationship(
            |  val namespace: String,
            |  val `object`: String,
            |  val relation: String,
            |  val subject_id: String?,
            |  val subject_set: SubjectSet?
            |)
        """.trimMargin()
    }

    @Test
    fun testCliOpenapiTypesScript() {
        val packageName = "community.flock.openapi"
        val packageDir = packageName.replace(".", "/")
        val input = "$inputDir/openapi/petstore.json"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert).main(
            arrayOf(
                "convert", "openapiv2",
                "-i", input,
                "-o", output,
                "-l", "TypeScript",
                "-p", "community.flock.openapi",
            ),
        )

        val directoryPath = DirectoryPath("$output/$packageDir")
        val path = FilePath(directoryPath, FileName("Petstore"))

        TypeScriptFile(path).read() shouldContain """
            |export type Pet = {
            |  "id": number | undefined,
            |  "category": Category | undefined,
            |  "name": string,
            |  "photoUrls": string[],
            |  "tags": Tag[] | undefined,
            |  "status": PetStatus | undefined
            |}
        """.trimMargin()
    }

    private fun getRandomString(length: Int) = (1..length)
        .map { allowedChars.random() }
        .joinToString("")

    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
}
