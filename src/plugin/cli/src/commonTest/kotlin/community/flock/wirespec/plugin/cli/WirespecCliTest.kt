package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Name
import community.flock.wirespec.plugin.io.read
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

        FilePath(directoryPath.resolve("model"), Name("Bla"), FileExtension.Kotlin).read() shouldBe """
            |package community.flock.wirespec.generated.model
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

        FilePath(directoryPath.resolve("model"), Name("Bla"), FileExtension.Java).read() shouldBe """
            |package community.flock.next.model;
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
        val path = FilePath(directoryPath.resolve("model"), Name("Pet"), FileExtension.Kotlin)

        path.read() shouldContain """
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
        val path = FilePath(directoryPath.resolve("model"), Name("Relationship"), FileExtension.Kotlin)

        path.read() shouldContain """
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
        val input = "$inputDir/openapi/petstore.json"
        val output = outputDir()

        WirespecCli.provide(::compile, ::convert).main(
            arrayOf(
                "convert",
                "openapiv2",
                "-i",
                input,
                "-o",
                output,
                "-l",
                "TypeScript",
            ),
        )

        val directoryPath = DirectoryPath("${output}/model")
        val path = FilePath(directoryPath,  Name("Pet"), FileExtension.TypeScript)

        path.read() shouldBe  """
            |export namespace Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
            |  export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
            |  export type Request<T> = { path: Record<string, unknown>, method: Method, queries?: Record<string, unknown>, headers?: Record<string, unknown>, body?:T }
            |  export type Response<T> = { status:number, headers?: Record<string, unknown>, body?:T }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |  export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }
            |}
            |
            |import {Category} from '../model/Category'
            |import {Tag} from '../model/Tag'
            |import {PetStatus} from '../model/PetStatus'
            |export type Pet = {
            |  "id": number | undefined,
            |  "category": Category | undefined,
            |  "name": string,
            |  "photoUrls": string[],
            |  "tags": Tag[] | undefined,
            |  "status": PetStatus | undefined
            |}
            |
        """.trimMargin()
    }

    private fun getRandomString(length: Int) = (1..length)
        .map { allowedChars.random() }
        .joinToString("")

    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
}
