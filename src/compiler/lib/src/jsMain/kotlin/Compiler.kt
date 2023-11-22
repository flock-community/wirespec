import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3

@JsExport
@ExperimentalJsExport
abstract class Compiler {

    protected fun preCompile(source: String) = Wirespec.compile(source)(logger)

    fun tokenize(source: String) = Wirespec.tokenize(source).produce()

    fun parse(source: String) = Wirespec.tokenize(source)
        .let { Parser(logger).parse(it) }

    companion object {
        protected val logger = object : Logger() {}
    }
}

@JsExport
@ExperimentalJsExport
class WsToKotlin : Compiler() {
    fun compile(source: String) = preCompile(source)(kotlinEmitter).produce()

    companion object {
        private val kotlinEmitter = KotlinEmitter(logger = logger)
    }
}

@JsExport
@ExperimentalJsExport
class WsToTypeScript : Compiler() {
    fun compile(source: String) = preCompile(source)(typeScriptEmitter).produce()

    companion object {
        private val typeScriptEmitter = TypeScriptEmitter(logger)
    }
}

@JsExport
@ExperimentalJsExport
class WsToWirespec : Compiler() {
    fun compile(source: String) = preCompile(source)(wirespecEmitter).produce()

    companion object {
        private val wirespecEmitter = WirespecEmitter(logger)
    }
}

interface ParserInterface {
    fun parse(source: String):Array<WsNode>
}

@JsExport
@ExperimentalJsExport
object OpenApiV2Parser{
    fun parse(source: String):Array<WsNode> = OpenApiParserV2.parse(source).produce()
}

@JsExport
@ExperimentalJsExport
object OpenApiV3Parser{
    fun parse(source: String):Array<WsNode> = OpenApiParserV3.parse(source).produce()
}


@JsExport
@ExperimentalJsExport
object OpenApiV3ToTypescript {
    val logger = object : Logger() {}
    private val emitter = TypeScriptEmitter(logger)
    fun compile (source: String): Array<WsCompiledFile> {
        val ast = OpenApiParserV3.parse(source)
        return emitter.emit(ast)
            .map { (file, value) -> WsCompiledFile(file, value)}
            .toTypedArray()
    }
}
