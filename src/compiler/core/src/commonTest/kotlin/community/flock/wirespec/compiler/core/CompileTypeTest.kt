package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileTypeTest {

    private val compiler = """
        |type Request {
        |  `type`: String,
        |  url: String,
        |  `BODY_TYPE`: String?,
        |  params: String[],
        |  headers: { String },
        |  body: { String?[]? }?
        |}
    """.trimMargin().let(::compile)

    @Test
    fun kotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |data class Request(
            |  val type: String,
            |  val url: String,
            |  val BODY_TYPE: String?,
            |  val params: List<String>,
            |  val headers: Map<String, String>,
            |  val body: Map<String, List<String?>?>?
            |)
            |
        """.trimMargin()

        compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |public record Request (
            |  String type,
            |  String url,
            |  java.util.Optional<String> BODY_TYPE,
            |  java.util.List<String> params,
            |  java.util.Map<String, String> headers,
            |  java.util.Optional<java.util.Map<String, java.util.Optional<java.util.List<java.util.Optional<String>>>>> body
            |) {
            |};
            |
        """.trimMargin()

        compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun scala() {
        val scala = """
            |package community.flock.wirespec.generated
            |
            |case class Request(
            |  val `type`: String,
            |  val url: String,
            |  val BODY_TYPE: Option[String],
            |  val params: List[String],
            |  val headers: Map[String, String],
            |  val body: Option[Map[String, Option[List[Option[String]]]]]
            |)
            |
        """.trimMargin()

        compiler { ScalaEmitter() } shouldBeRight scala
    }

    @Test
    fun typeScript() {
        val ts = """
            |export type Request = {
            |  "type": string,
            |  "url": string,
            |  "BODY_TYPE": string | undefined,
            |  "params": string[],
            |  "headers": Record<string, string>,
            |  "body": Record<string, string | undefined[] | undefined> | undefined
            |}
            |
            |
        """.trimMargin()

        compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun wireSpec() {
        val wirespec = """
            |type Request {
            |  `type`: String,
            |  url: String,
            |  `BODY_TYPE`: String?,
            |  params: String[],
            |  headers: { String },
            |  body: { String?[]? }?
            |}
            |
        """.trimMargin()

        compiler { WirespecEmitter() } shouldBeRight wirespec
    }
}
