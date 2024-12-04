@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.npm

import arrow.core.curried
import community.flock.kotlinx.openapi.bindings.v2.SwaggerObject
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.lib.WsNode
import community.flock.wirespec.compiler.lib.WsStringResult
import community.flock.wirespec.compiler.lib.consume
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.generator.generate
import community.flock.wirespec.openapi.v2.OpenApiV2Emitter
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Emitter
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import community.flock.wirespec.plugin.cli.main
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

@JsExport
enum class Emitters {
    WIRESPEC,
    TYPESCRIPT,
    JAVA,
    KOTLIN,
    SCALA,
    OPENAPI_V2,
    OPENAPI_V3,
}

@JsExport
enum class Converters {
    OPENAPI_V2,
    OPENAPI_V3,
}

@JsExport
fun cli(args: Array<String>) = main(args)

@JsExport
fun tokenize(source: String) = WirespecSpec
    .tokenize(source)
    .map { it.produce() }
    .toTypedArray()

@JsExport
fun parse(source: String) = WirespecSpec
    .tokenize(source)
    .let(Parser(noLogger)::parse)
    .produce()

@JsExport
fun convert(source: String, converters: Converters) = when (converters) {
    Converters.OPENAPI_V2 -> OpenApiV2Parser.parse(source).produce()
    Converters.OPENAPI_V3 -> OpenApiV3Parser.parse(source).produce()
}

@JsExport
fun generate(source: String, type: String): WsStringResult = WirespecSpec
    .tokenize(source)
    .let(Parser(noLogger)::parse)
    .map { it.generate(type).toString() }
    .produce()

@JsExport
fun emit(ast: Array<WsNode>, emitter: Emitters, packageName: String) = ast
    .map { it.consume() }
    .let {
        when (emitter) {
            Emitters.WIRESPEC -> WirespecEmitter().emit(it)
            Emitters.TYPESCRIPT -> TypeScriptEmitter().emit(it)
            Emitters.JAVA -> JavaEmitter(packageName).emit(it)
            Emitters.KOTLIN -> KotlinEmitter(packageName).emit(it)
            Emitters.SCALA -> ScalaEmitter(packageName).emit(it)
            Emitters.OPENAPI_V2 -> listOf(it)
                .map(OpenApiV2Emitter::emitSwaggerObject)
                .map(encode(SwaggerObject.serializer()))
                .map(::Emitted.curried()("openapi")::invoke)

            Emitters.OPENAPI_V3 -> listOf(it)
                .map{ast -> OpenApiV3Emitter.emitOpenAPIObject(ast, null)}
                .map(encode(OpenAPIObject.serializer()))
                .map(::Emitted.curried()("openapi")::invoke)
        }
    }
    .map { it.produce() }
    .toTypedArray()

private fun <T> encode(serializer: SerializationStrategy<T>) = { value: T -> Json.encodeToString(serializer, value) }
