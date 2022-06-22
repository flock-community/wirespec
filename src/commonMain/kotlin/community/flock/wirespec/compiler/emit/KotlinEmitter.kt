package community.flock.wirespec.compiler.emit

import community.flock.wirespec.compiler.emit.common.Emitter
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition.Shape.Value.Custom
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition.Shape.Value.Ws

object KotlinEmitter : Emitter() {

    override fun TypeDefinition.emit(): String = withLogging {
        "data class ${name.emit()}(\n${shape.emit()}\n)\n\n"
    }

    override fun TypeDefinition.Name.emit(): String = withLogging { value }

    override fun TypeDefinition.Shape.emit(): String = withLogging {
        value.map { (key, value) -> "${SPACER}val ${key.emit()}: ${value.emit()}," }
            .joinToString("\n")
            .dropLast(1)
    }

    override fun TypeDefinition.Shape.Key.emit(): String = withLogging { value }

    override fun TypeDefinition.Shape.Value.emit(): String = withLogging {
        when (this) {
            is Custom -> value
            is Ws -> when (value) {
                Ws.Type.String -> "String"
                Ws.Type.Integer -> "Int"
            }
        }
    }

}
