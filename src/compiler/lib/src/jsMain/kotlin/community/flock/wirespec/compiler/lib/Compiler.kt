@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser

@JsExport
abstract class Compiler {

    protected fun preCompile(source: String) = WirespecSpec.compile(source)(noLogger)

    fun tokenize(source: String) = WirespecSpec.tokenize(source).produce()

    fun parse(source: String) = WirespecSpec.tokenize(source)
        .let { Parser(noLogger).parse(it).produce() }
}

@JsExport
class WsToKotlin : Compiler() {
    fun compile(source: String) = preCompile(source)(kotlinEmitter).produce()

    companion object {
        private val kotlinEmitter = KotlinEmitter(logger = noLogger)
    }
}

@JsExport
class WsToTypeScript : Compiler() {
    fun compile(source: String) = preCompile(source)(typeScriptEmitter).produce()

    companion object {
        private val typeScriptEmitter = TypeScriptEmitter(noLogger)
    }
}

@JsExport
class WsToScala : Compiler() {
    fun compile(source: String) = preCompile(source)(typeScriptEmitter).produce()

    companion object {
        private val typeScriptEmitter = ScalaEmitter(logger = noLogger)
    }
}

@JsExport
class WsToJava : Compiler() {
    fun compile(source: String) = preCompile(source)(typeScriptEmitter).produce()

    companion object {
        private val typeScriptEmitter = JavaEmitter(logger = noLogger)
    }
}

@JsExport
class WsToWirespec : Compiler() {
    fun compile(source: String) = preCompile(source)(wirespecEmitter).produce()

    companion object {
        private val wirespecEmitter = WirespecEmitter(noLogger)
    }
}

interface ParserInterface {
    fun parse(source: String): Array<WsNode>
}

@JsExport
object OpenApiV2Parser {
    fun parse(source: String): Array<WsNode> = OpenApiV2Parser.parse(source).produce()
}


@JsExport
object OpenApiV2ToTypescript {
    private val emitter = TypeScriptEmitter(noLogger)
    fun compile(source: String): Array<WsCompiledFile> {
        val ast = OpenApiV2Parser.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value) }
            .toTypedArray()
    }
}

@JsExport
object OpenApiV2ToWirespec {
    private val emitter = WirespecEmitter(noLogger)
    fun compile(source: String): Array<WsCompiledFile> {
        val ast = OpenApiV2Parser.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value) }
            .toTypedArray()
    }
}


@JsExport
object OpenApiV3Parser {
    fun parse(source: String): Array<WsNode> = OpenApiV3Parser.parse(source).produce()
}

@JsExport
object OpenApiV3ToTypescript {
    private val emitter = TypeScriptEmitter(noLogger)
    fun compile(source: String): Array<WsCompiledFile> {
        val ast = OpenApiV3Parser.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value) }
            .toTypedArray()
    }
}

@JsExport
object OpenApiV3ToWirespec {
    private val emitter = WirespecEmitter(noLogger)
    fun compile(source: String): Array<WsCompiledFile> {
        val ast = OpenApiV3Parser.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value) }
            .toTypedArray()
    }
}
