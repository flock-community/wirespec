package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.utils.Logger

private val logger = object : Logger(false) {}

@JsExport
@ExperimentalJsExport
class WsToKotlin {
    fun compile(source: String) = WireSpec.compile(source)(logger)(kotlinEmitter)

    companion object {
        private val kotlinEmitter = KotlinEmitter(logger)
    }
}

@JsExport
@ExperimentalJsExport
class WsToTypeScript {
    fun compile(source: String) = WireSpec.compile(source)(logger)(typeScriptEmitter)

    companion object {
        private val typeScriptEmitter = TypeScriptEmitter(logger)
    }
}
