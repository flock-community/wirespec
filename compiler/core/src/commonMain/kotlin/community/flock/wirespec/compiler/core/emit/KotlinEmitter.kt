package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Ws
import community.flock.wirespec.compiler.utils.Logger

class KotlinEmitter(logger: Logger) : Emitter(logger) {

    override fun Type.emit(): String = withLogging(logger) {
        "data class ${name.emit()}(\n${shape.emit()}\n)\n\n"
    }

    override fun Type.Name.emit(): String = withLogging(logger) { value }

    override fun Type.Shape.emit(): String = withLogging(logger) {
        value.map { (key, value) -> "${SPACER}val ${key.emit()}: ${value.emit()}," }
            .joinToString("\n")
            .dropLast(1)
    }

    override fun Type.Shape.Key.emit(): String = withLogging(logger) { value }

    override fun Type.Shape.Value.emit(): String = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Ws -> when (value) {
                Ws.Type.String -> "String"
                Ws.Type.Integer -> "Int"
                Ws.Type.Boolean -> "Boolean"
            }
        }
    }

}
