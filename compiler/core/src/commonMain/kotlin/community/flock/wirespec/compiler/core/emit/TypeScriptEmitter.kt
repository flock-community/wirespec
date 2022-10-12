package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.Shape
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Ws
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

class TypeScriptEmitter(logger: Logger) : Emitter(logger) {

    override fun Type.emit() = withLogging(logger) {
        "interface ${name.emit()} {\n${shape.emit()}\n}\n\n"
    }

    override fun Type.Name.emit() = withLogging(logger) { value }

    override fun Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Shape.Field.emit() = withLogging(logger) {
        "${SPACER}${key.emit()}${if (isNullable) "?" else ""}: ${value.emit()},"
    }

    override fun Shape.Field.Key.emit() = withLogging(logger) { value }

    override fun Shape.Field.Value.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Ws -> when (value) {
                Ws.Type.String -> "string"
                Ws.Type.Integer -> "number"
                Ws.Type.Boolean -> "boolean"
            }
        }.let { if (isIterable) "$it[]" else it }
    }

}
