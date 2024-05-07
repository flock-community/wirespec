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
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser

@JsExport
abstract class Compiler {

    protected fun preCompile(source: String) = WirespecSpec.compile(source)(logger)

    fun tokenize(source: String) = WirespecSpec.tokenize(source).produce()

    fun parse(source: String) = WirespecSpec.tokenize(source)
        .let { Parser(logger).parse(it).produce() }

    companion object {
        protected val logger = object : Logger() {}
    }
}

@JsExport
class WsToKotlin : Compiler() {
    fun compile(source: String) = preCompile(source)(kotlinEmitter).produce()

    companion object {
        private val kotlinEmitter = KotlinEmitter(logger = logger)
    }
}

@JsExport
class WsToTypeScript : Compiler() {
    fun compile(source: String) = preCompile(source)(typeScriptEmitter).produce()

    companion object {
        private val typeScriptEmitter = TypeScriptEmitter(logger)
    }
}

@JsExport
class WsToScala : Compiler() {
    fun compile(source: String) = preCompile(source)(typeScriptEmitter).produce()

    companion object {
        private val typeScriptEmitter = ScalaEmitter(logger = logger)
    }
}

@JsExport
class WsToJava : Compiler() {
    fun compile(source: String) = preCompile(source)(typeScriptEmitter).produce()

    companion object {
        private val typeScriptEmitter = JavaEmitter(logger = logger)
    }
}

@JsExport
class WsToWirespec : Compiler() {
    fun compile(source: String) = preCompile(source)(wirespecEmitter).produce()

    companion object {
        private val wirespecEmitter = WirespecEmitter(logger)
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
    val logger = object : Logger() {}
    private val emitter = TypeScriptEmitter(logger)
    fun compile(source: String): Array<WsCompiledFile> {
        val ast = OpenApiV2Parser.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value) }
            .toTypedArray()
    }
}

@JsExport
object OpenApiV2ToWirespec {
    val logger = object : Logger() {}
    private val emitter = WirespecEmitter(logger)
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
    val logger = object : Logger() {}
    private val emitter = TypeScriptEmitter(logger)
    fun compile(source: String): Array<WsCompiledFile> {
        val ast = OpenApiV3Parser.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value) }
            .toTypedArray()
    }
}

@JsExport
object OpenApiV3ToWirespec {
    val logger = object : Logger() {}
    private val emitter = WirespecEmitter(logger)
    fun compile(source: String): Array<WsCompiledFile> {
        val ast = OpenApiV3Parser.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value) }
            .toTypedArray()
    }
}
