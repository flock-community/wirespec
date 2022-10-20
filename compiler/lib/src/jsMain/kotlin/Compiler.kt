import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.Logger

@JsExport
@ExperimentalJsExport
abstract class Compiler {

    protected fun preCompile(source: String) = WireSpec.compile(source)(logger)

    fun tokenize(source: String) = WireSpec.tokenize(source).produce()

    fun parse(source: String) = WireSpec.tokenize(source)
        .let { Parser(logger).parse(it) }
        .fold(
            ifRight = { ast -> ast },
            ifLeft = { err -> throw err }
        )

    companion object {
        protected val logger = object : Logger(false) {}
    }
}

@JsExport
@ExperimentalJsExport
class WsToKotlin : Compiler() {
    fun compile(source: String) = preCompile(source)(kotlinEmitter).produce()

    companion object {
        private val kotlinEmitter = KotlinEmitter(logger)
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
