package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Value.Ws
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class JavaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger, true) {

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName;\n\n$result" }

    override fun Type.emit() = withLogging(logger) {
        "public record ${name.emit()}(\n${shape.emit()}\n) {};\n\n"
    }

    override fun Type.TName.emit() = withLogging(logger) { value }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "$SPACER${if (isNullable) "java.util.Optional<${value.emit()}>" else value.emit()} ${key.emit()},"
    }

    override fun Type.Shape.Field.Key.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Value.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Ws -> when (value) {
                Ws.Type.String -> "String"
                Ws.Type.Integer -> "Integer"
                Ws.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "java.util.List<$it>" else it }
    }

    override fun Refined.emit() = withLogging(logger) {
        "public record ${name.emit()}(String value) {\n${SPACER}static void validate(${name.emit()} record) {\n$SPACER${validator.emit()}\n$SPACER}\n}\n"
    }

    override fun Refined.RName.emit() = withLogging(logger) { value }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "${SPACER}java.util.regex.Pattern.compile($value).matcher(record.value).find();"
    }
}
