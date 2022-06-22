package community.flock.wirespec.compiler.emit

import community.flock.wirespec.compiler.emit.common.Emitter
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition

object TypeScriptEmitter : Emitter() {

    override fun TypeDefinition.emit(): String = withLogging {
        "interface ${name.emit()} {\n${shape.emit()}\n}\n\n"
    }

    override fun TypeDefinition.Name.emit(): String = withLogging {
        value
    }

    override fun TypeDefinition.Shape.emit(): String = withLogging {
        value.map { (key, value) -> "${SPACER}${key.emit()}: ${value.emit()}," }
            .joinToString("\n")
            .dropLast(1)
    }

    override fun TypeDefinition.Shape.Key.emit(): String = withLogging {
        value
    }

    override fun TypeDefinition.Shape.Value.emit(): String = withLogging {
        when (this) {
            is TypeDefinition.Shape.Value.Custom -> value
            is TypeDefinition.Shape.Value.Ws -> when (value) {
                TypeDefinition.Shape.Value.Ws.Type.String -> "string"
                TypeDefinition.Shape.Value.Ws.Type.Integer -> "number"
            }
        }
    }

}
