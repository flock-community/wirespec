package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class TypeScriptEmitter(logger: Logger = noLogger) : Emitter(logger) {

    override fun Type.emit() = withLogging(logger) {
        """interface $name {
            |${shape.emit()}
            |}
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${SPACER}${identifier.emit()}${if (isNullable) "?" else ""}: ${reference.emit()},"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (type) {
                Primitive.Type.String -> "string"
                Primitive.Type.Integer -> "number"
                Primitive.Type.Boolean -> "boolean"
            }
        }.let { if (isIterable) "$it[]" else it }
    }

    override fun Refined.emit() = withLogging(logger) {
        """interface $name {
            |${SPACER}value: string
            |}
            |const validate$name = (type: $name) => (${validator.emit()}).test(type.value);
            |
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "new RegExp('${value.drop(1).dropLast(1)}')"
    }

    override fun Endpoint.emit() = withLogging(logger) {
        """// TODO("Not yet implemented")"""
    }
}
