package community.flock.wirespec.compiler.core.emit

import arrow.core.Validated
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Shape.Field
import community.flock.wirespec.compiler.core.parse.Shape.Field.*
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class JavaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger, true) {

    override fun emit(ast: AST): Validated<WirespecException.CompilerException, List<Pair<String, String>>> =
        super.emit(ast).map {
            it.map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName;\n\n$result" }
        }

    override fun TypeDefinition.emit() = withLogging(logger) {
        "public record ${name.emit()}(\n${shape.emit()}\n) {};\n\n"
    }

    override fun TypeDefinition.Name.emit() = withLogging(logger) { value }

    override fun Type.emit() = withLogging(logger) {
        when(this){
            is Shape -> this.emit()
            is Value -> TODO()
        }
    }

    override fun Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Field.emit() = withLogging(logger) {
        "$SPACER${if (isNullable) "java.util.Optional<${value.emit()}>" else value.emit()} ${key.emit()},"
    }

    override fun Key.emit() = withLogging(logger) { value }

    override fun Value.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (value) {
                Primitive.PrimitiveType.String -> "String"
                Primitive.PrimitiveType.Integer -> "Integer"
                Primitive.PrimitiveType.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "List<$it>" else it }
    }

    override fun EndpointDefinition.emit(): String {
        TODO("Not yet implemented")
    }

    override fun EndpointDefinition.Response.emit(className: String): String {
        TODO("Not yet implemented")
    }

}
