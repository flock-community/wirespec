package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class WirespecEmitter(logger: Logger = noLogger) : Emitter(logger) {

    override val shared = ""

    override fun Type.emit() = withLogging(logger) {
        """type $name {
            |${shape.emit()}
            |}
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
            is Reference.Any -> "Any"
            is Reference.Custom -> value
            is Reference.Primitive -> when (type) {
                Reference.Primitive.Type.String -> "String"
                Reference.Primitive.Type.Integer -> "Integer"
                Reference.Primitive.Type.Number -> "Number"
                Reference.Primitive.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "$it[]" else it }
    }

    override fun Enum.emit() = withLogging(logger) { "enum $name {\n${SPACER}${entries.joinToString(", ")}\n}\n" }

    override fun Refined.emit() = withLogging(logger) {
        "refined $name ${validator.emit()}\n"
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "/${value.drop(1).dropLast(1)}/g"
    }

    override fun Endpoint.emit() = withLogging(logger) {
        """// TODO("Not yet implemented")
          |
        """.trimMargin()
    }
}
