@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.npm

import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.OpenAPIV2ModelSerializer
import community.flock.kotlinx.openapi.bindings.OpenAPIV3ModelSerializer
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.lib.WsAST
import community.flock.wirespec.compiler.lib.WsEmitted
import community.flock.wirespec.compiler.lib.WsStringResult
import community.flock.wirespec.compiler.lib.consume
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.converter.avro.AvroJsonEmitter
import community.flock.wirespec.converter.avro.AvroJsonParser
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.generator.generate
import community.flock.wirespec.ir.generator.JavaGenerator
import community.flock.wirespec.ir.generator.KotlinGenerator
import community.flock.wirespec.ir.generator.PythonGenerator
import community.flock.wirespec.ir.generator.TypeScriptGenerator
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
    KOTLIN(KotlinIrEmitter(emitShared = EmitShared(true)).emitShared()!!.let(KotlinGenerator::generate)),
    JAVA(JavaIrEmitter(emitShared = EmitShared(true)).emitShared()!!.let(JavaGenerator::generate)),
    TYPESCRIPT(TypeScriptIrEmitter().emitShared()!!.let(TypeScriptGenerator::generate)),
    PYTHON(PythonIrEmitter(emitShared = EmitShared(true)).emitShared()!!.let(PythonGenerator::generate)),
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
fun startLsp(useNodeIpc: Boolean) = community.flock.wirespec.lsp.startLsp(useNodeIpc)

@JsExport
fun tokenize(source: String) = WirespecSpec
    .tokenize(source)
    .map { it.produce() }
    .toTypedArray()

@JsExport
fun parse(source: String) = object : ParseContext, NoLogger {}.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).produce()

@JsExport
fun convert(source: String, converters: Converters, strict: Boolean = false) = when (converters) {
    Converters.OPENAPI_V2 -> OpenAPIV2Parser.parse(ModuleContent(FileUri(""), source), strict).produce()
    Converters.OPENAPI_V3 -> OpenAPIV3Parser.parse(ModuleContent(FileUri(""), source), strict).produce()
    Converters.AVRO -> AvroJsonParser.parse(ModuleContent(FileUri(""), source), strict).produce()
}

@JsExport
fun generate(source: String, type: String): WsStringResult = object : ParseContext, NoLogger {}
    .parse(nonEmptyListOf(ModuleContent(FileUri(""), source)))
    .map { it.generate(type).toString() }
    .produce()

@JsExport
fun emit(wsAst: WsAST, emitter: Emitters, packageName: String, emitShared: Boolean): Array<WsEmitted> {
    val ast = wsAst.consume()
    return when (emitter) {
        Emitters.WIRESPEC -> WirespecEmitter().emit(ast, noLogger)
        Emitters.TYPESCRIPT -> TypeScriptIrEmitter().emit(ast, noLogger)
        Emitters.JAVA -> JavaIrEmitter(PackageName(packageName), EmitShared(emitShared)).emit(ast, noLogger)
        Emitters.KOTLIN -> KotlinIrEmitter(PackageName(packageName), EmitShared(emitShared)).emit(ast, noLogger)
        Emitters.PYTHON -> PythonIrEmitter(PackageName(packageName), EmitShared(emitShared)).emit(ast, noLogger)
        Emitters.OPENAPI_V2 ->
            OpenAPIV2Emitter
                .emitSwaggerObject(ast.modules.flatMap { it.statements }, noLogger)
                .let(encode(OpenAPIV2ModelSerializer))
                .let { Emitted("openapi.json", it) }
                .let { nonEmptyListOf(it) }
        Emitters.OPENAPI_V3 ->
            OpenAPIV3Emitter
                .emitOpenAPIObject(ast.modules.flatMap { it.statements }, null, noLogger)
                .let(encode(OpenAPIV3ModelSerializer))
                .let { Emitted("openapi.json", it) }
                .let { nonEmptyListOf(it) }
        Emitters.AVRO ->
            ast.modules
                .map { ast -> AvroJsonEmitter.emit(ast) }
                .map { Json.encodeToString(it) }
                .map { Emitted("avro.json", it) }
    }
        .map { it.produce() }
        .toTypedArray()
}

private fun <T> encode(serializer: SerializationStrategy<T>) = { value: T -> Json.encodeToString(serializer, value) }
