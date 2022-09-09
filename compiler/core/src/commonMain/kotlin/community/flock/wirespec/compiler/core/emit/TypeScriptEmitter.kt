package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Ws
import community.flock.wirespec.compiler.utils.Logger

class TypeScriptEmitter(logger: Logger) : Emitter(logger) {

    override fun Type.emit(): String = withLogging(logger) {
        "interface ${name.emit()} {\n${shape.emit()}\n}\n\n"
    }

    override fun Type.Name.emit(): String = withLogging(logger) { value }

    override fun Type.Shape.emit(): String = withLogging(logger) {
        value.map { (key, value) -> "${SPACER}${key.emit(key.nullable)}: ${value.emit()}," }
            .joinToString("\n")
            .dropLast(1)
    }

    override fun Type.Shape.Key.emit(nullable: Boolean): String = withLogging(logger) { if (nullable)"$value?" else value }

    override fun Type.Shape.Value.emit(): String = withLogging(logger) {
        when (this) {
            is Custom -> emit(value)
            is Ws -> when (value) {
                Ws.Type.String -> emit("string")
                Ws.Type.Integer -> emit("number")
                Ws.Type.Boolean -> emit("boolean")
            }
        }
    }

    private fun Type.Shape.Value.emit(s: String) = when {
        iterable -> "$s[]"
        else -> s
    }

}
