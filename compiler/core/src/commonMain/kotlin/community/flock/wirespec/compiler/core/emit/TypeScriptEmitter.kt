package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Value.Ws
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class TypeScriptEmitter(logger: Logger = noLogger) : Emitter(logger) {

    override fun Type.emit() = withLogging(logger) {
        "interface ${name.value} {\n${shape.emit()}\n}\n\n"
    }

    override fun Type.Name.emit() = withLogging(logger) { value }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${SPACER}${key.emit()}${if (isNullable) "?" else ""}: ${value.emit()},"
    }

    override fun Type.Shape.Field.Key.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Value.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Ws -> when (value) {
                Ws.Type.String -> "string"
                Ws.Type.Integer -> "number"
                Ws.Type.Boolean -> "boolean"
            }
        }.let { if (isIterable) "$it[]" else it }
    }

    override fun Refined.emit() = withLogging(logger) {
        "interface ${name.emit()} {\n${SPACER}value: string\n}\nconst validate${name.emit()} = (type: ${name.emit()}) => (${validator.emit()}).test(type.value);\n\n"
    }

    override fun Refined.Name.emit() = withLogging(logger) { value }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "new RegExp('${value.drop(1).dropLast(1)}')"
    }
}
