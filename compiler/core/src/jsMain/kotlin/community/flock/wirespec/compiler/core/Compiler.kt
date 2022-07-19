package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.utils.Logger

private val noopLogger = object : Logger(false) {}

@JsExport
@ExperimentalJsExport
class Compiler {
    fun toKotlin(source: String) = WireSpec.compile(source)(noopLogger)(KotlinEmitter(noopLogger))
    fun toTypeScript(source: String) = WireSpec.compile(source)(noopLogger)(TypeScriptEmitter(noopLogger))
}
