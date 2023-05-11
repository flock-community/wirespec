package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Endpoint
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

    override fun Endpoint.emit(): String {
        val params = this.path.filterIsInstance<Endpoint.Segment.Param>()
            .map {
                when (it.reference) {
                    is Custom -> it.key to it.reference.value
                    is Primitive -> it.key to it.reference.type.name
                }
            }
            .joinToString(", ") { (k, v) -> "$k: $v" }
        val className = this.name
        return "interface ${this.name} {\n" +
                "\tsealed interface ${className}Response\n" +
                this.responses.map { it.emit(className) }.joinToString("\n") + "\n" +
                "\tfun ${this.name}($params):${this.name}Response\n" +
                "}\n\n"
    }

    override fun Endpoint.Response.emit(className: String) =
        "\tdata class ${className}Response${
            contentType.replace(
                "/",
                ""
            )
        }${status}(val status:Int, val contentType:String, val content: ${type.toName()}): ${className}Response"

    private fun Type.Shape.Field.Reference.toName() = when(this){
        is Primitive -> type.name.toIterable(isIterable)
        is Custom ->  value.toIterable(isIterable)
    }

    private fun String.toIterable(isIterable: Boolean) = when(isIterable){
        true -> "List<$this>"
        else -> this
    }
}
