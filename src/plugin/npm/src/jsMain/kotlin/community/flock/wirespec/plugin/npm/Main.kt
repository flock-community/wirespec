@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.npm

import Generator.generate
import community.flock.kotlinx.openapi.bindings.v2.SwaggerObject
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.lib.WsNode
import community.flock.wirespec.compiler.lib.WsStringResult
import community.flock.wirespec.compiler.lib.consume
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.v2.OpenApiV2Emitter
import community.flock.wirespec.openapi.v3.OpenApiV3Emitter
import community.flock.wirespec.plugin.cli.main
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@JsExport
enum class Emitters {
    TYPESCRIPT,
    JAVA,
    KOTLIN,
    SCALA,
    OPENAPI_V2,
    OPENAPI_V3,
}

@JsExport
fun cli(args: Array<String>) = main(args)

@JsExport
fun parse(source: String) = WirespecSpec
    .tokenize(source)
    .let { Parser(noLogger).parse(it).produce() }

@JsExport
fun generate(source: String, type: String): WsStringResult = WirespecSpec
    .tokenize(source)
    .let { Parser(noLogger).parse(it) }
    .map { it.generate(type).toString() }
    .produce()

@JsExport
fun emit(ast: Array<WsNode>, emitter: Emitters, packageName: String) = ast
    .map { it.consume() }
    .let {

        when (emitter) {
            Emitters.TYPESCRIPT -> TypeScriptEmitter().emit(it)
            Emitters.JAVA -> JavaEmitter(packageName).emit(it)
            Emitters.KOTLIN -> KotlinEmitter(packageName).emit(it)
            Emitters.SCALA -> ScalaEmitter(packageName).emit(it)
            Emitters.OPENAPI_V2 -> {
                val res = OpenApiV2Emitter().emit(it)
                val json = Json.encodeToJsonElement(SwaggerObject.serializer(), res)
                listOf(Emitted("openapi", json.toString()))
            }
            Emitters.OPENAPI_V3 -> {
                val res = OpenApiV3Emitter().emit(it)
                val json = Json.encodeToJsonElement(OpenAPIObject.serializer(), res)
                listOf(Emitted("openapi", json.toString()))
            }
        }
    }
    .map { it.produce() }
    .toTypedArray()
