@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.npm

import arrow.core.curried
import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.v2.SwaggerObject
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.lib.WsAST
import community.flock.wirespec.compiler.lib.WsStringResult
import community.flock.wirespec.compiler.lib.consume
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.converter.avro.AvroEmitter
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.java.JavaShared
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.kotlin.KotlinShared
import community.flock.wirespec.emitters.python.PythonEmitter
import community.flock.wirespec.emitters.python.PythonShared
import community.flock.wirespec.emitters.typescript.TypeScriptEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptShared
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.generator.generate
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.cli.main
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@JsExport
enum class Shared(val source: String) {
    KOTLIN(KotlinShared.source),
    JAVA(JavaShared.source),
    TYPESCRIPT(TypeScriptShared.source),
    PYTHON(PythonShared.source),
}

@JsExport
enum class Emitters {
    WIRESPEC,
    TYPESCRIPT,
    JAVA,
    KOTLIN,
    PYTHON,
    OPENAPI_V2,
    OPENAPI_V3,
    AVRO,
}

@JsExport
enum class Converters {
    OPENAPI_V2,
    OPENAPI_V3,
    AVRO,
}

@JsExport
fun cli(args: Array<String>) = main(args)

@JsExport
fun tokenize(source: String) = WirespecSpec
    .tokenize(source)
    .map { it.produce() }
    .toTypedArray()

@JsExport
fun parse(source: String) = object : ParseContext, NoLogger {}.parse(nonEmptyListOf(ModuleContent("", source))).produce()

@JsExport
fun convert(source: String, converters: Converters) = when (converters) {
    Converters.OPENAPI_V2 -> OpenAPIV2Parser.parse(ModuleContent("", source), true).produce()
    Converters.OPENAPI_V3 -> OpenAPIV3Parser.parse(ModuleContent("", source), true).produce()
    Converters.AVRO -> AvroParser.parse(ModuleContent("", source), true).produce()
}

@JsExport
fun generate(source: String, type: String): WsStringResult = object : ParseContext, NoLogger {}
    .parse(nonEmptyListOf(ModuleContent("", source)))
    .map { it.generate(type).toString() }
    .produce()

@JsExport
fun emit(ast: WsAST, emitter: Emitters, packageName: String, emitShared: Boolean) = ast
    .modules
    .map { module -> module.consume() }
    .flatMap {
        when (emitter) {
            Emitters.WIRESPEC -> WirespecEmitter().emit(it, noLogger)
            Emitters.TYPESCRIPT -> TypeScriptEmitter(EmitShared(emitShared)).emit(it, noLogger)
            Emitters.JAVA -> JavaEmitter(PackageName(packageName), EmitShared(emitShared)).emit(it, noLogger)
            Emitters.KOTLIN -> KotlinEmitter(PackageName(packageName), EmitShared(emitShared)).emit(it, noLogger)
            Emitters.PYTHON -> PythonEmitter(PackageName(packageName), EmitShared(emitShared)).emit(it, noLogger)
            Emitters.OPENAPI_V2 -> listOf(it)
                .map(OpenAPIV2Emitter::emitSwaggerObject)
                .map(encode(SwaggerObject.serializer()))
                .map(::Emitted.curried()("openapi")::invoke)

            Emitters.OPENAPI_V3 -> listOf(it)
                .map { ast -> OpenAPIV3Emitter.emitOpenAPIObject(ast, null) }
                .map(encode(OpenAPIObject.serializer()))
                .map(::Emitted.curried()("openapi")::invoke)

            Emitters.AVRO -> listOf(it)
                .map { ast -> AvroEmitter.emit(ast) }
                .map { Json.encodeToString(it) }
                .map(::Emitted.curried()("avro")::invoke)
        }
    }
    .map { it.produce() }
    .toTypedArray()

private fun <T> encode(serializer: SerializationStrategy<T>) = { value: T -> Json.encodeToString(serializer, value) }
