package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileTypeTest {

    private val compiler = compile(
        """
        type Request {
          `type`: String,
          url: String,
          body: String?,
          params: String[],
          headers: { String }
        }
        """.trimIndent()
    )

    @Test
    fun kotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |data class Request(
            |  val type: String,
            |  val url: String,
            |  val body: String? = null,
            |  val params: List<String>,
            |  val headers: Map<String, String>
            |)
            |
        """.trimMargin()

        compiler(KotlinEmitter()) shouldBeRight kotlin
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |public record Request (
            |  String type,
            |  String url,
            |  java.util.Optional<String> body,
            |  java.util.List<String> params,
            |  java.util.Map<String, String> headers
            |) {
            |};
            |
        """.trimMargin()

        compiler(JavaEmitter()) shouldBeRight java
    }

    @Test
    fun scala() {
        val scala = """
            |package community.flock.wirespec.generated
            |
            |case class Request(
            |  val `type`: String,
            |  val url: String,
            |  val body: Option[String],
            |  val params: List[String],
            |  val headers: Map[String, String]
            |)
            |
            |
        """.trimMargin()

        compiler(ScalaEmitter()) shouldBeRight scala
    }

    @Test
    fun typeScript() {
        val ts = """
            |export type Request = {
            |  "type": string,
            |  "url": string,
            |  "body"?: string,
            |  "params": string[],
            |  "headers": Record<string, string>
            |}
            |
            |
        """.trimMargin()

        compiler(TypeScriptEmitter()) shouldBeRight ts
    }

    @Test
    fun wireSpec() {
        val wirespec = """
            |type Request {
            |  `type`: String,
            |  url: String,
            |  body: String?,
            |  params: String[],
            |  headers: { String }
            |}
            |
        """.trimMargin()

        compiler(WirespecEmitter()) shouldBeRight wirespec
    }
}
