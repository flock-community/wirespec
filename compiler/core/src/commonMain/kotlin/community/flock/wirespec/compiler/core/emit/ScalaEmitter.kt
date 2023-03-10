package community.flock.wirespec.compiler.core.emit

import arrow.core.Validated
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class ScalaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger) {

    override fun emit(ast: AST): Validated<WirespecException.CompilerException, List<Pair<String, String>>> =
        super.emit(ast).map {
            it.map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName\n\n$result" }
        }

    override fun TypeDefinition.emit() = withLogging(logger) {
        "case class ${name.emit()}(\n${shape.emit()}\n)\n\n"
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
        "${SPACER}val ${key.emit()}: ${if (isNullable) "Option[${value.emit()}]" else value.emit()},"
    }

    override fun Shape.Field.Key.emit() = withLogging(logger) { value }

    override fun Shape.Field.Value.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (value) {
                Primitive.PrimitiveType.String -> "String"
                Primitive.PrimitiveType.Integer -> "Int"
                Primitive.PrimitiveType.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "List[$it]" else it }
    }

    override fun EndpointDefinition.emit(): String {
        TODO("Not yet implemented")
    }

    override fun EndpointDefinition.Response.emit(className: String): String {
        TODO("Not yet implemented")
    }

}
