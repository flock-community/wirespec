package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class JavaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger, true) {

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName;\n\n$result" }

    override fun Type.emit() = withLogging(logger) {
        """public record $name(
            |${shape.emit()}
            |) {};
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "$SPACER${if (isNullable) "java.util.Optional<${reference.emit()}>" else reference.emit()} ${identifier.emit()},"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (type) {
                Primitive.Type.String -> "String"
                Primitive.Type.Integer -> "Integer"
                Primitive.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "java.util.List<$it>" else it }
    }

    override fun Refined.emit() = withLogging(logger) {
        """public record $name(String value) {
            |${SPACER}static void validate($name record) {
            |${SPACER}${validator.emit()}
            |${SPACER}}
            |}
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "${SPACER}java.util.regex.Pattern.compile($value).matcher(record.value).find();"
    }

    override fun Endpoint.emit(): String {
        TODO("Not yet implemented")
    }

    override fun Endpoint.Response.emit(className: String): String {
        TODO("Not yet implemented")
    }

}
