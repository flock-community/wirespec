package community.flock.wirespec.compiler.emit

import community.flock.wirespec.compiler.emit.common.Emitter
import community.flock.wirespec.compiler.parse.Type
import community.flock.wirespec.compiler.parse.Type.Shape.Value.Custom
import community.flock.wirespec.compiler.parse.Type.Shape.Value.Ws

object TypeScriptEmitter : Emitter() {

    override fun Type.emit(): String = withLogging {
        "interface ${name.emit()} {\n${shape.emit()}\n}\n\n"
    }

    override fun Type.Name.emit(): String = withLogging { value }

    override fun Type.Shape.emit(): String = withLogging {
        value.map { (key, value) -> "${SPACER}${key.emit()}: ${value.emit()}," }
            .joinToString("\n")
            .dropLast(1)
    }

    override fun Type.Shape.Key.emit(): String = withLogging { value }

    override fun Type.Shape.Value.emit(): String = withLogging {
        when (this) {
            is Custom -> value
            is Ws -> when (value) {
                Ws.Type.String -> "string"
                Ws.Type.Integer -> "number"
                Ws.Type.Boolean -> "boolean"
            }
        }
    }

}
