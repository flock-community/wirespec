package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class KotlinEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger) {

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName\n\n$result" }

    override fun Type.emit() = withLogging(logger) {
        """data class $name(
            |${shape.emit()}
            |)
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${SPACER}val ${identifier.emit()}: ${reference.emit()}${if (isNullable) "?" else ""},"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (type) {
                Primitive.Type.String -> "String"
                Primitive.Type.Integer -> "Int"
                Primitive.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "List<$it>" else it }
    }

    override fun Refined.emit() = withLogging(logger) {
        """data class $name(val value: String)
            |fun $name.validate() = ${validator.emit()}
            |
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) { "Regex($value).find(value)" }
}
