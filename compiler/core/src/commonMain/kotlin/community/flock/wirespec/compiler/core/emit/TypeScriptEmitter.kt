package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.EndpointDefinition
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.TypeDefinition
import community.flock.wirespec.compiler.core.parse.Shape
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class TypeScriptEmitter(logger: Logger = noLogger) : Emitter(logger) {

    override fun TypeDefinition.emit() = withLogging(logger) {
        "interface ${name.emit()} {\n${shape.emit()}\n}\n\n"
    }

    override fun TypeDefinition.Name.emit() = withLogging(logger) { value }

    override fun Type.emit() = withLogging(logger) {
        when(this){
            is Shape -> this.emit()
            is Shape.Field.Value -> TODO()
        }
    }

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
            is Primitive -> when (value) {
                Primitive.PrimitiveType.String -> "string"
                Primitive.PrimitiveType.Integer -> "number"
                Primitive.PrimitiveType.Boolean -> "boolean"
            }
        }.let { if (isIterable) "$it[]" else it }
    }

    override fun EndpointDefinition.emit(): String {
        TODO("Not yet implemented")
    }

    override fun EndpointDefinition.Response.emit(className: String): String {
        TODO("Not yet implemented")
    }
}
