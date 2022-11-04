package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.exceptions.WireSpecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Value.Ws
import community.flock.wirespec.compiler.utils.Logger

class ScalaEmitter(private val packageName: String = DEFAULT_PACKAGE_NAME, logger: Logger) : Emitter(logger) {

    override fun emit(ast: AST): Either<WireSpecException.CompilerException, List<Pair<String, String>>> =
        super.emit(ast).map {
            it.map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName\n\n$result" }
        }

    override fun Type.emit() = withLogging(logger) {
        "case class ${name.emit()}(\n${shape.emit()}\n)\n\n"
    }

    override fun Type.Name.emit() = withLogging(logger) { value }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${SPACER}val ${key.emit()}: ${if (isNullable) "Option[${value.emit()}]" else value.emit()},"
    }

    override fun Type.Shape.Field.Key.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Value.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Ws -> when (value) {
                Ws.Type.String -> "String"
                Ws.Type.Integer -> "Int"
                Ws.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "List[$it]" else it }
    }

}
