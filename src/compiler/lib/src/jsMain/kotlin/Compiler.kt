import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.Logger

@JsExport
@ExperimentalJsExport
abstract class Compiler {

    protected fun preCompile(source: String) = Wirespec.compile(source)(logger)

    fun tokenize(source: String) = Wirespec.tokenize(source).produce()

    fun parse(source: String) = Wirespec.tokenize(source)
        .let { Parser(logger).parse(it) }
        .let { Ast(arrayOf()) }

    companion object {
        protected val logger = object : Logger(false) {}
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
